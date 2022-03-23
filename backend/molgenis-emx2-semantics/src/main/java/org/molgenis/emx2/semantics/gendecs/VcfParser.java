package org.molgenis.emx2.semantics.gendecs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class VcfParser {
  File vcfFile;
  StarRating starRating;
  ArrayList<String> hpoTerms;

  public VcfParser(String filename, StarRating starRating, ArrayList<String> hpoTerms) {
    vcfFile = new File(filename);
    this.starRating = starRating;
    this.hpoTerms = hpoTerms;
  }

  private ArrayList<String> getRating(StarRating starRating) {
    switch (starRating) {
      case ZEROSTAR -> {
        return new ArrayList<>(List.of("no_assertion_provided", "no_assertion_criteria_provided"));
      }
      case ONESTAR -> {
        return new ArrayList<>(
            List.of(
                "criteria_provided,_single_submitter",
                "criteria_provided,_conflicting_interpretations",
                "no_assertion_provided",
                "no_assertion_criteria_provided"));
      }
      case TWOSTAR -> {
        return new ArrayList<>(
            List.of(
                "criteria_provided,_multiple_submitters,_no_conflicts",
                "criteria_provided,_single_submitter",
                "criteria_provided,_conflicting_interpretations",
                "no_assertion_provided",
                "no_assertion_criteria_provided"));
      }
      case THREESTAR -> {
        return new ArrayList<>(
            List.of(
                "reviewed_by_expert_panel",
                "criteria_provided,_multiple_submitters,_no_conflicts",
                "criteria_provided,_single_submitter",
                "criteria_provided,_conflicting_interpretations",
                "no_assertion_provided",
                "no_assertion_criteria_provided"));
      }
    }
    return null;
  }

  /**
   * Method removeStatus removes given starRating from input clinvar file.
   *
   * @param inputFile clinvar file as vcf
   * @return boolean true if successful, false if failed
   */
  public String removeStatus(String inputFile) {
    String pathName = String.format("data/gendecs/Filtered_Clinvar_%s.vcf", this.starRating);
    Path path = Paths.get("data/gendecs/Filtered_ClinVar.vcf");
    if(path.toFile().isFile()) {
      return pathName;
    } else {
      File filteredClinVar = new File(pathName);
      try {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filteredClinVar));
        File fileObject = new File(inputFile);
        Scanner reader = new Scanner(fileObject);
        while (reader.hasNextLine()) {
          String data = reader.nextLine();
          if (stringContainsItemFromList(data, Objects.requireNonNull(getRating(this.starRating)))) {
            continue;
          }
          writer.write(data + System.getProperty("line.separator"));
        }
        reader.close();
        writer.close();
        return pathName;
      } catch (IOException e) {
        e.printStackTrace();
      }
      return "";
    }
  }

  private static boolean stringContainsItemFromList(String inputString, ArrayList<String> items) {
    return items.stream().anyMatch(inputString::contains);
  }

  /**
   * Method matchWithClinvar reads given vcf file with variants and builds regex for each line.
   * Which is then used to match this with variants in the clinvar vcf file. The found matches are
   * filtered on the variant being pathogenic or not. The remaining variants are added to the
   * Variants class.
   *
   * @return Variants class with the matched variants
   */
  public Variants matchWithClinvar(String pathName) {
    Map<String, Pattern> stringsToFind = new HashMap<>();
    try {
      Scanner reader = new Scanner(vcfFile);
      while (reader.hasNextLine()) {
        String data = reader.nextLine();
        if (data.startsWith("#")) {
          continue;
        }
        String[] splittedData = data.split("\t");
        String chromosome = splittedData[0];
        String position = splittedData[1];
        String ref = splittedData[3];
        String alt = splittedData[4];
        Pattern pattern =
            Pattern.compile(
                String.format("%s\\t%s\\t[0-9]+\\t%s\\t%s.+", chromosome, position, ref, alt));
        stringsToFind.put(data, pattern);
      }
      reader.close();

      return getMatchesClinvar(stringsToFind, pathName);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private Variants getMatchesClinvar(Map<String, Pattern> stringsToFind, String pathName) throws IOException {
    File file = new File(pathName);
    Scanner reader = new Scanner(file);

    File resultFileClinvar = new File("data/gendecs/result_matches_clinvar.vcf");
    File resultFileResult = new File("data/gendecs/result_matches.vcf");
    BufferedWriter writerResult = new BufferedWriter(new FileWriter(resultFileResult));
    BufferedWriter writerClinvar = new BufferedWriter(new FileWriter(resultFileClinvar));

    writeHeader(writerClinvar, "clinvar");
    writeHeader(writerResult, "result");

    Variants variants = new Variants();
    VariantHpoMatcher variantHpoMatcher = new VariantHpoMatcher(hpoTerms, variants);

    while (reader.hasNextLine()) {
      String currentLine = reader.nextLine();
      for (Pattern stringToFind : stringsToFind.values()) {
        if (currentLine.matches(String.valueOf(stringToFind))) {
          if (isPathogenic(currentLine)) {
            if (variantHpoMatcher.matchVariantWithHpo(currentLine)) {
              writerResult.write(
                  getKeyFromValue(stringsToFind, stringToFind)
                      + System.getProperty("line.separator"));
              writerClinvar.write(currentLine + System.getProperty("line.separator"));
              variants.addVariant(currentLine);
            }
          }
        }
      }
    }
    reader.close();
    writerClinvar.close();
    writerResult.close();
    return variants;
  }

  private void writeHeader(BufferedWriter writer, String headerType) throws IOException {
    if (headerType.equals("clinvar")) {
      for (String line : VcfFile.getClinvarHeader().split("\n")) {
        writer.write(line + System.getProperty("line.separator"));
      }
    } else if (headerType.equals("result")) {
      for (String line : VcfFile.getVipHeader().split("\n")) {
        writer.write(line + System.getProperty("line.separator"));
      }
    } else {
      System.out.println("invalid header argument " + headerType);
    }
  }

  private static boolean isPathogenic(String variant) {
    ArrayList<String> clinSig =
        new ArrayList<>(List.of("likely_pathogenic", "pathogenic", "pathogenic/likely_pathogenic"));
    String[] splittedLine = variant.split("\t");
    String[] infoString = splittedLine[7].split(";");

    for (String i : infoString) {
      if (i.contains("CLNSIG")) {
        if (clinSig.contains(i.split("=")[1].toLowerCase())) {
          return true;
        }
      }
    }
    return false;
  }

  private static String getKeyFromValue(Map<String, Pattern> map, Pattern value) {

    return map.entrySet().stream()
        .filter(entry -> Objects.equals(entry.getValue(), value))
        .map(Map.Entry::getKey)
        .findFirst()
        .map(Object::toString)
        .orElse("");
  }
}
