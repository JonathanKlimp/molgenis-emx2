package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.FilterBean.*;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.transform;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.typeForMutationResult;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;
import static org.molgenis.emx2.sql.SqlQuery.*;

import graphql.Scalars;
import graphql.schema.*;
import java.util.*;
import java.util.stream.Collectors;
import org.molgenis.emx2.*;

public class GraphqlTableFieldFactory {
  // static types
  private static final GraphQLEnumType orderByEnum =
      GraphQLEnumType.newEnum()
          .name("MolgenisOrderByEnum")
          .value(Order.ASC.name(), Order.ASC)
          .value(Order.DESC.name(), Order.DESC)
          .build();
  private static GraphQLObjectType fileDownload =
      GraphQLObjectType.newObject()
          .name("MolgenisFileDownload")
          .field(GraphQLFieldDefinition.newFieldDefinition().name("id").type(Scalars.GraphQLString))
          .field(GraphQLFieldDefinition.newFieldDefinition().name("size").type(Scalars.GraphQLInt))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("extension")
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name("url").type(Scalars.GraphQLString))
          .build();
  final List<String> agg_fields = List.of("max", "min", "sum", "avg");
  // cache so we can reuse filter input types between tables
  private Map<String, GraphQLInputObjectType> tableFilterInputTypes = new LinkedHashMap<>();
  // cache so we can reuse filter input types between tables
  private Map<ColumnType, GraphQLInputObjectType> columnFilterInputTypes = new LinkedHashMap<>();
  private Map<String, GraphQLInputObjectType> rowInputTypes = new LinkedHashMap<>();
  private Map<String, GraphQLInputObjectType> refTypes = new LinkedHashMap<>();

  public static String escape(String value) {
    if (value != null) {
      String result =
          value
              // make sure there are now leading/trailing spaces
              .trim()
              // replace all _ with __
              .replaceAll("_", "__")
              // replace multiple spaces with 1 space
              .replaceAll(" +", " ")
              // replace all spaces with _
              .replaceAll(" ", "_");
      if (result.endsWith("__agg")) {
        result =
            // fix pitfall of unneeded escape
            result.replaceAll("__agg", "_agg");
      }
      if (result.contains("mg__")) {
        result =
            // fix pitfall of unneeded escape
            result.replaceAll("mg__", "mg_");
      }
      return result;
    } else {
      return null;
    }
  }

  // schema specific types
  public GraphQLFieldDefinition tableQueryField(Table table) {
    GraphQLObjectType tableType = createTableObjectType(table);
    return GraphQLFieldDefinition.newFieldDefinition()
        .name(escape(table.getName()))
        .type(GraphQLList.list(tableType))
        .dataFetcher(fetcherForTableQueryField(table))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.FILTER_ARGUMENT)
                .type(getTableFilterInputObjectType(table.getMetadata()))
                .build())
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.SEARCH)
                .type(Scalars.GraphQLString)
                .build())
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.LIMIT)
                .type(Scalars.GraphQLInt)
                .build())
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.OFFSET)
                .type(Scalars.GraphQLInt)
                .build())
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.ORDERBY)
                .type(createTableOrderByInputObjectType(table))
                .build())
        .build();
  }

  public GraphQLFieldDefinition tableAggField(Table table) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name(escape(table.getName()) + "_agg")
        .type(createTableAggregationType(table))
        .dataFetcher(fetcherForTableQueryField(table))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.FILTER_ARGUMENT)
                .type(getTableFilterInputObjectType(table.getMetadata()))
                .build())
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.SEARCH)
                .type(Scalars.GraphQLString)
                .build())
        .build();
  }

  private GraphQLObjectType createTableObjectType(Table table) {
    GraphQLObjectType.Builder tableBuilder =
        GraphQLObjectType.newObject().name(escape(table.getName()));
    for (Column col : table.getMetadata().getColumnsWithoutHeadings()) {
      String id = escape(col.getName());
      switch (col.getColumnType().getBaseType()) {
        case HEADING:
          // nothing to do
          break;
        case FILE:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition().name(id).type(fileDownload));
          break;
        case BOOL:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition().name(id).type(Scalars.GraphQLBoolean));
          break;
        case BOOL_ARRAY:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(id)
                  .type(GraphQLList.list(Scalars.GraphQLBoolean)));
          break;
        case STRING:
        case TEXT:
        case LONG:
        case UUID:
        case DATE:
        case DATETIME:
        case EMAIL:
        case HYPERLINK:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition().name(id).type(Scalars.GraphQLString));
          break;
        case STRING_ARRAY:
        case EMAIL_ARRAY:
        case HYPERLINK_ARRAY:
        case TEXT_ARRAY:
        case LONG_ARRAY:
        case DATE_ARRAY:
        case DATETIME_ARRAY:
        case UUID_ARRAY:
        case JSONB_ARRAY:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(id)
                  .type(GraphQLList.list(Scalars.GraphQLString)));
          break;
        case DECIMAL:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition().name(id).type(Scalars.GraphQLFloat));
          break;
        case DECIMAL_ARRAY:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(id)
                  .type(GraphQLList.list(Scalars.GraphQLBigDecimal)));
          break;
        case INT:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition().name(id).type(Scalars.GraphQLInt));
          break;
        case INT_ARRAY:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(id)
                  .type(GraphQLList.list(Scalars.GraphQLInt)));
          break;
        case REF:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(id)
                  .type(GraphQLTypeReference.typeRef(escape(col.getRefTableName()))));
          break;
        case REF_ARRAY:
        case REFBACK:
          // case MREF:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(id)
                  .type(
                      GraphQLList.list(GraphQLTypeReference.typeRef(escape(col.getRefTableName()))))
                  .argument(
                      GraphQLArgument.newArgument()
                          .name(GraphqlConstants.LIMIT)
                          .type(Scalars.GraphQLInt)
                          .build())
                  .argument(
                      GraphQLArgument.newArgument()
                          .name(GraphqlConstants.OFFSET)
                          .type(Scalars.GraphQLInt)
                          .build())
                  .argument(
                      GraphQLArgument.newArgument()
                          .name(GraphqlConstants.ORDERBY)
                          .type(
                              GraphQLTypeReference.typeRef(
                                  escape(col.getRefTableName()) + GraphqlConstants.ORDERBY))
                          .build()));
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(id + "_agg")
                  .type(GraphQLTypeReference.typeRef(escape(col.getRefTableName()) + "Aggregate")));
          break;
        default:
          throw new UnsupportedOperationException(
              "Not yet implemented type " + col.getColumnType());
      }
    }
    return tableBuilder.build();
  }

  private GraphQLObjectType createTableAggregationType(Table table) {
    String tableName = escape(table.getName());
    GraphQLObjectType.Builder builder = GraphQLObjectType.newObject().name(tableName + "Aggregate");
    builder.field(
        GraphQLFieldDefinition.newFieldDefinition().name("count").type(Scalars.GraphQLInt));
    List<Column> aggCols =
        table.getMetadata().getColumns().stream()
            .filter(
                c ->
                    ColumnType.INT.equals(c.getColumnType())
                        || ColumnType.DECIMAL.equals(c.getColumnType())
                        || ColumnType.LONG.equals(c.getColumnType()))
            .toList();

    if (aggCols.size() > 0) {
      GraphQLObjectType.Builder max = GraphQLObjectType.newObject().name(tableName + "_max");
      GraphQLObjectType.Builder min = GraphQLObjectType.newObject().name(tableName + "_min");
      GraphQLObjectType.Builder sum = GraphQLObjectType.newObject().name(tableName + "_sum");
      GraphQLObjectType.Builder avg = GraphQLObjectType.newObject().name(tableName + "_avg");
      for (Column col : aggCols) {
        max.field(
            GraphQLFieldDefinition.newFieldDefinition()
                .name(escape(col.getName()))
                .type(graphQLTypeOf(col)));
        min.field(
            GraphQLFieldDefinition.newFieldDefinition()
                .name(escape(col.getName()))
                .type(graphQLTypeOf(col)));
        avg.field(
            GraphQLFieldDefinition.newFieldDefinition()
                .name(escape(col.getName()))
                .type(Scalars.GraphQLFloat));
        sum.field(
            GraphQLFieldDefinition.newFieldDefinition()
                .name(escape(col.getName()))
                .type(graphQLTypeOf(col)));
      }
      builder
          .field(GraphQLFieldDefinition.newFieldDefinition().name(MAX_FIELD).type(max))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(MIN_FIELD).type(min))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(AVG_FIELD).type(avg))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(SUM_FIELD).type(sum));
    }
    // todo group by options
    return builder.build();
  }

  private GraphQLInputObjectType getTableFilterInputObjectType(TableMetadata table) {
    String tableName = escape(table.getTableName());
    if (!tableFilterInputTypes.containsKey(tableName)) {
      String typeName = tableName + FILTER;
      GraphQLInputObjectType.Builder filterBuilder =
          GraphQLInputObjectType.newInputObject().name(typeName);
      if (table.getPrimaryKeyColumns().size() > 0) {
        filterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(FILTER_EQUALS)
                .type(GraphQLList.list(getPrimaryKeyInput(table)))
                .build());
      }
      filterBuilder.field(
          GraphQLInputObjectField.newInputObjectField()
              .name(FILTER_SEARCH)
              .type(Scalars.GraphQLString)
              .build());
      filterBuilder.field(
          GraphQLInputObjectField.newInputObjectField()
              .name(FILTER_OR)
              .type(GraphQLList.list(GraphQLTypeReference.typeRef(typeName)))
              .build());
      filterBuilder.field(
          GraphQLInputObjectField.newInputObjectField()
              .name(FILTER_AND)
              .type(GraphQLList.list(GraphQLTypeReference.typeRef(typeName)))
              .build());
      for (Column col : table.getColumns()) {
        if (col.isReference()) {
          filterBuilder.field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(escape(col.getName()))
                  .type(GraphQLTypeReference.typeRef(escape(col.getRefTableName()) + FILTER))
                  .build());
        } else if (col.getColumnType().getOperators().length > 0) {
          filterBuilder.field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(escape(col.getName()))
                  .type(getColumnFilterInputObjectType(col))
                  .build());
        }
      }
      tableFilterInputTypes.put(tableName, filterBuilder.build());
    }
    return tableFilterInputTypes.get(tableName);
  }

  private GraphQLInputObjectType createTableOrderByInputObjectType(Table table) {
    GraphQLInputObjectType.Builder orderByBuilder =
        GraphQLInputObjectType.newInputObject()
            .name(escape(table.getName()) + GraphqlConstants.ORDERBY);
    for (Column col : table.getMetadata().getColumns()) {
      orderByBuilder.field(
          GraphQLInputObjectField.newInputObjectField()
              .name(escape(col.getName()))
              .type(orderByEnum));
    }
    return orderByBuilder.build();
  }

  private GraphQLInputObjectType getColumnFilterInputObjectType(Column column) {
    ColumnType type = column.getColumnType();
    // singleton
    if (this.columnFilterInputTypes.get(type) == null) {
      String typeName = type.toString().toLowerCase();
      typeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
      GraphQLInputObjectType.Builder builder =
          GraphQLInputObjectType.newInputObject().name("Molgenis" + typeName + FILTER);
      for (Operator operator : type.getOperators()) {
        builder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(operator.getName())
                .type(GraphQLList.list(graphQLTypeOf(column))));
      }
      columnFilterInputTypes.put(type, builder.build());
    }
    return columnFilterInputTypes.get(type);
  }

  private GraphQLScalarType graphQLTypeOf(Column col) {
    switch (col.getColumnType().getBaseType()) {
      case BOOL, BOOL_ARRAY:
        return Scalars.GraphQLBoolean;
      case INT, INT_ARRAY:
        return Scalars.GraphQLInt;
      case LONG, LONG_ARRAY:
        return Scalars.GraphQLLong;
      case DECIMAL, DECIMAL_ARRAY:
        return Scalars.GraphQLFloat;
      case DATE,
          DATETIME,
          STRING,
          TEXT,
          UUID,
          DATE_ARRAY,
          DATETIME_ARRAY,
          STRING_ARRAY,
          TEXT_ARRAY,
          EMAIL_ARRAY,
          HYPERLINK_ARRAY:
      case UUID_ARRAY:
        return Scalars.GraphQLString;
      case REF_ARRAY, REF, REFBACK:
      default:
        throw new UnsupportedOperationException("Type not supported yet: " + col.getColumnType());
    }
  }

  private FilterBean[] convertMapToFilterArray(Table table, Map<String, Object> filter) {
    List<Filter> subFilters = new ArrayList<>();
    for (Map.Entry<String, Object> entry : filter.entrySet()) {
      if (entry.getKey().equals(FILTER_OR) || entry.getKey().equals(FILTER_AND)) {
        List<Map<String, Object>> nested = (List<Map<String, Object>>) entry.getValue();
        List<Filter> nestedFilters =
            nested.stream()
                .map(m -> and(convertMapToFilterArray(table, m)))
                .collect(Collectors.toList());
        if (entry.getKey().equals(FILTER_OR)) {
          subFilters.add(or(nestedFilters.toArray(new Filter[nestedFilters.size()])));
        } else {
          subFilters.add(and(nestedFilters.toArray(new Filter[nestedFilters.size()])));
        }
      } else if (entry.getKey().equals(FILTER_SEARCH)) {
        subFilters.add(f(Operator.TRIGRAM_SEARCH, entry.getValue()));
      } else if (entry.getKey().equals(FILTER_EQUALS)) {
        //  complex filter, should be an list of maps per graphql contract
        if (entry.getValue() != null) {
          subFilters.add(
              or(
                  ((List<Map<String, Object>>) entry.getValue())
                      .stream().map(v -> createKeyFilter(v)).collect(Collectors.toList())));
        }
      } else {
        // find column by escaped name
        Optional<Column> optional =
            table.getMetadata().getColumns().stream()
                .filter(c -> escape(c.getName()).equals(entry.getKey()))
                .findFirst();
        if (!optional.isPresent())
          throw new GraphqlException(
              "Graphql API error: Column "
                  + entry.getKey()
                  + " unknown in table "
                  + table.getName());
        Column c = optional.get();
        if (c.isReference()) {
          subFilters.add(
              f(
                  c.getName(),
                  convertMapToFilterArray(
                      table
                          .getSchema()
                          .getDatabase()
                          .getSchema(c.getRefTable().getSchemaName())
                          .getTable(c.getRefTableName()),
                      (Map) entry.getValue())));
        } else {
          subFilters.add(convertMapToFilter(c.getName(), (Map<String, Object>) entry.getValue()));
        }
      }
    }
    return subFilters.toArray(new FilterBean[subFilters.size()]);
  }

  private Filter createKeyFilter(Map<String, Object> map) {
    List<Filter> result = new ArrayList<>();
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (entry.getValue() instanceof Map) {
        result.add(f(entry.getKey(), createKeyFilter((Map<String, Object>) entry.getValue())));
      } else {
        result.add(f(entry.getKey(), Operator.EQUALS, entry.getValue()));
      }
    }
    return and(result);
  }

  private Filter convertMapToFilter(String name, Map<String, Object> subFilter) {
    int count = 0;
    for (Map.Entry<String, Object> entry2 : subFilter.entrySet()) {
      count++;
      if (count > 1)
        throw new MolgenisException("Can only have one operator, found multiple for " + name);
      Operator op = Operator.fromAbbreviation(entry2.getKey());
      if (entry2.getValue() instanceof List) {
        return f(name, op, (List) entry2.getValue());
      } else {
        return f(name, op, entry2.getValue());
      }
    }
    return null;
  }

  /** creates a list like List.of(field1,field2, path1, List.of(pathsubfield1), ...) */
  private SelectColumn[] convertMapSelection(
      TableMetadata aTable, DataFetchingFieldSelectionSet selection) {
    List<SelectColumn> result = new ArrayList<>();
    for (SelectedField s : selection.getFields()) {
      if (!s.getQualifiedName().contains("/")) {
        if (s.getSelectionSet().getFields().isEmpty()) {
          Optional<Column> column = findColumnById(aTable, s.getName());
          if (column.isPresent()) {
            result.add(new SelectColumn(column.get().getName()));
          } else {
            result.add(new SelectColumn(s.getName()));
          }
        } else {
          Optional<Column> column = findColumnById(aTable, s.getName());
          if (column.isPresent()) {
            SelectColumn sc =
                new SelectColumn(
                    column.get().getName() + (s.getName().endsWith("_agg") ? "_agg" : ""),
                    convertMapSelection(column.get().getRefTable(), s.getSelectionSet()));
            // get limit and offset for the selection
            Map<String, Object> args = s.getArguments();
            if (args.containsKey(GraphqlConstants.LIMIT)) {
              sc.setLimit((int) args.get(GraphqlConstants.LIMIT));
            }
            if (args.containsKey(GraphqlConstants.OFFSET)) {
              sc.setOffset((int) args.get(GraphqlConstants.OFFSET));
            }
            if (args.containsKey(GraphqlConstants.ORDERBY)) {
              sc.setOrderBy((Map<String, Order>) args.get(GraphqlConstants.ORDERBY));
            }
            result.add(sc);
          } else if (agg_fields.contains(s.getName())) {
            result.add(
                new SelectColumn(s.getName(), convertMapSelection(aTable, s.getSelectionSet())));
          }
        }
      }
    }
    return result.toArray(new SelectColumn[result.size()]);
  }

  private Optional<Column> findColumnById(TableMetadata aTable, String id) {
    if (aTable != null) {
      return aTable.getColumns().stream()
          .filter(c -> escape(c.getName()).equals(id) || (escape(c.getName()) + "_agg").equals(id))
          .findFirst();
    } else {
      return Optional.empty();
    }
  }

  private DataFetcher fetcherForTableQueryField(Table aTable) {
    return dataFetchingEnvironment -> {
      Table table = aTable;
      Query q = table.query();
      String fieldName = dataFetchingEnvironment.getField().getName();
      if (fieldName.endsWith("_agg")) {
        q = table.agg();
      }
      q.select(
          convertMapSelection(aTable.getMetadata(), dataFetchingEnvironment.getSelectionSet()));
      Map<String, Object> args = dataFetchingEnvironment.getArguments();
      if (dataFetchingEnvironment.getArgument(GraphqlConstants.FILTER_ARGUMENT) != null) {
        q.where(
            convertMapToFilterArray(
                table, dataFetchingEnvironment.getArgument(GraphqlConstants.FILTER_ARGUMENT)));
      }
      if (args.containsKey(GraphqlConstants.LIMIT)) {
        q.limit((int) args.get(GraphqlConstants.LIMIT));
      }
      if (args.containsKey(GraphqlConstants.OFFSET)) {
        q.offset((int) args.get(GraphqlConstants.OFFSET));
      }
      if (args.containsKey(GraphqlConstants.ORDERBY)) {
        q.orderBy((Map<String, Order>) args.get(GraphqlConstants.ORDERBY));
      }

      String search = dataFetchingEnvironment.getArgument(GraphqlConstants.SEARCH);
      if (search != null && !search.trim().equals("")) {
        q.search(search);
      }

      Object result = transform(q.retrieveJSON());
      // bit silly, we have to remove root field here. Some refactoring makes this look nicer
      if (result != null) return ((Map<String, Object>) result).get(fieldName);
      return null;
    };
  }

  private GraphQLFieldDefinition getMutationDefinition(Schema schema, MutationType type) {
    GraphQLFieldDefinition.Builder fieldBuilder =
        GraphQLFieldDefinition.newFieldDefinition()
            .name(type.name().toLowerCase())
            .type(typeForMutationResult)
            .dataFetcher(fetcher(schema, type));
    for (TableMetadata table : schema.getMetadata().getTablesIncludingExternal()) {
      if (table.getColumnsWithoutHeadings().size() > 0) {
        fieldBuilder.argument(
            GraphQLArgument.newArgument()
                .name(escape(table.getTableName()))
                .type(GraphQLList.list(rowInputType(table.getTable()))));
      }
    }
    return fieldBuilder.build();
  }

  public GraphQLFieldDefinition insertMutation(Schema schema) {
    return getMutationDefinition(schema, MutationType.INSERT);
  }

  public GraphQLFieldDefinition updateMutation(Schema schema) {
    return getMutationDefinition(schema, MutationType.UPDATE);
  }

  public GraphQLFieldDefinition upsertMutation(Schema schema) {
    return getMutationDefinition(schema, MutationType.SAVE);
  }

  public GraphQLFieldDefinition deleteMutation(Schema schema) {
    GraphQLFieldDefinition.Builder fieldBuilder =
        GraphQLFieldDefinition.newFieldDefinition()
            .name("delete")
            .type(typeForMutationResult)
            .dataFetcher(fetcher(schema, MutationType.DELETE));

    for (Table table : schema.getTablesSorted()) {
      // if no pkey is provided, you cannot delete rows
      if (!schema.getMetadata().getTableMetadata(table.getName()).getPrimaryKeys().isEmpty()) {
        fieldBuilder.argument(
            GraphQLArgument.newArgument()
                .name(escape(table.getName()))
                // reuse same input as insert
                .type(
                    GraphQLList.list(
                        GraphQLTypeReference.typeRef(escape(table.getName()) + INPUT))));
      }
    }
    return fieldBuilder.build();
  }

  private DataFetcher fetcher(Schema schema, MutationType mutationType) {
    return dataFetchingEnvironment -> {
      StringBuilder result = new StringBuilder();
      boolean any = false;
      for (TableMetadata tableMetadata : schema.getMetadata().getTablesIncludingExternal()) {
        List<Map<String, Object>> rowsAslistOfMaps =
            dataFetchingEnvironment.getArgument(escape(tableMetadata.getTableName()));
        if (rowsAslistOfMaps != null) {
          String tableName = tableMetadata.getTableName();
          Table table = tableMetadata.getTable();
          int count = 0;
          switch (mutationType) {
            case UPDATE:
              count =
                  table.update(
                      GraphqlApiFactory.convertToRows(table.getMetadata(), rowsAslistOfMaps));
              result.append("updated " + count + " records to " + tableName + "\n");
              break;
            case INSERT:
              count =
                  table.insert(
                      GraphqlApiFactory.convertToRows(table.getMetadata(), rowsAslistOfMaps));
              result.append("inserted " + count + " records to " + tableName + "\n");
              break;
            case SAVE:
              count =
                  table.save(
                      GraphqlApiFactory.convertToRows(table.getMetadata(), rowsAslistOfMaps));
              result.append("upserted " + count + " records to " + tableName + "\n");
              break;
            case DELETE:
              count =
                  table.delete(
                      GraphqlApiFactory.convertToRows(table.getMetadata(), rowsAslistOfMaps));
              result.append("delete " + count + " records from " + tableName + "\n");
              break;
          }
          any = true;
        }
      }
      if (!any) throw new MolgenisException("Error with save: no data provided");
      return new GraphqlApiMutationResult(SUCCESS, result.toString());
    };
  }

  private GraphQLInputObjectType rowInputType(Table table) {
    String tableName = escape(table.getName());
    if (rowInputTypes.get(tableName) == null) {
      GraphQLInputObjectType.Builder inputBuilder =
          GraphQLInputObjectType.newInputObject().name(tableName + INPUT);
      for (Column col : table.getMetadata().getColumnsWithoutHeadings()) {
        GraphQLInputType type;
        if (col.isReference()) {
          if (col.isRef()) {
            type = getPrimaryKeyInput(col.getRefTable());
          } else {
            type = GraphQLList.list(getPrimaryKeyInput(col.getRefTable()));
          }
        } else {
          ColumnType columnType = col.getPrimitiveColumnType();
          type = getGraphQLInputType(columnType);
        }
        inputBuilder.field(
            GraphQLInputObjectField.newInputObjectField().name(escape(col.getName())).type(type));
      }
      rowInputTypes.put(tableName, inputBuilder.build());
    }
    return rowInputTypes.get(tableName);
  }

  public GraphQLInputObjectType getPrimaryKeyInput(TableMetadata table) {
    GraphQLInputType type;
    String name = escape(table.getTableName()) + "PkeyInput";
    if (refTypes.get(name) == null) {
      GraphQLInputObjectType.Builder refTypeBuilder =
          GraphQLInputObjectType.newInputObject().name(name);
      for (Column ref : table.getPrimaryKeyColumns()) {
        ColumnType columnType = ref.getColumnType();
        if (ref.isReference()) {
          type = getPrimaryKeyInput(ref.getRefTable());
        } else {
          type = getGraphQLInputType(columnType);
        }
        refTypeBuilder.field(
            GraphQLInputObjectField.newInputObjectField().name(escape(ref.getName())).type(type));
      }
      refTypes.put(name, refTypeBuilder.build());
    }
    return refTypes.get(name);
  }

  private GraphQLInputType getGraphQLInputType(ColumnType columnType) {
    return switch (columnType.getBaseType()) {
      case FILE -> GraphqlCustomTypes.GraphQLFileUpload;
      case BOOL -> Scalars.GraphQLBoolean;
      case INT -> Scalars.GraphQLInt;
      case LONG -> Scalars.GraphQLLong; // NOSONAR
      case DECIMAL -> Scalars.GraphQLFloat;
      case UUID, STRING, TEXT, DATE, DATETIME -> Scalars.GraphQLString;
      case BOOL_ARRAY -> GraphQLList.list(Scalars.GraphQLBoolean);
      case INT_ARRAY -> GraphQLList.list(Scalars.GraphQLInt);
      case LONG_ARRAY -> GraphQLList.list(Scalars.GraphQLLong); // NOSONAR
      case DECIMAL_ARRAY -> GraphQLList.list(Scalars.GraphQLFloat);
      case STRING_ARRAY,
          TEXT_ARRAY,
          DATE_ARRAY,
          DATETIME_ARRAY,
          UUID_ARRAY,
          EMAIL_ARRAY,
          HYPERLINK_ARRAY -> GraphQLList.list(Scalars.GraphQLString);
      default -> throw new MolgenisException(
          "Internal error: Type " + columnType + " not expected at this place");
    };
  }
}
