# Do the following to run GenDecS:

## Files needed to run GenDecS:
* ClinVar: clinvar_20220528.vcf.gz from: https://ftp.ncbi.nlm.nih.gov/pub/clinvar/vcf_GRCh37/ 
* HPO ontology: version used during developing: https://github.com/obophenotype/human-phenotype-ontology/blob/v2022-04-14/hp.owl   
   or latest version: https://hpo.jax.org/app/download/ontology 
* Genes_to_phenotype.txt: version used during developing: https://github.com/obophenotype/human-phenotype-ontology/releases/tag/v2022-04-14.   
   or latest version: https://hpo.jax.org/app/download/annotation 

## Creation of database
1. Clone emx2 (https://github.com/JonathanKlimp/molgenis-emx2)
2. Create directory gendecs in the folder data
3. Place the HPO ontology and genes_to_phenotype.txt in /data/gendecs/
4. Follow the installation steps of EMX2 (https://molgenis.github.io/molgenis-emx2/#/molgenis/run_java / https://molgenis.github.io/molgenis-emx2/#/molgenis/dev_quickstart)
5. Navigate to http://localhost:8080
6. Login to EMX2 with username:admin password:admin
7. Go to the Databases tab or navigate to http://localhost:8080/apps/central/#/
8. Create a database called "gendecs"
9. Inside the database gendecs import the Patients.csv table found here: https://github.com/JonathanKlimp/molgenis-emx2/tree/master/apps/gendecs/data

## Upload data to the database
10. Download each file from: https://github.com/JonathanKlimp/GenDecS-tools/tree/main/gendecsTestData
11. Download each file from: https://github.com/JonathanKlimp/GenDecS-tools/tree/main/builds
12. Place the data in a seperate folder.
13. Open the terminal in this folder.
14. Execute the following command:
    for FILE in *; do sh pathTo/GenDecS_filter_and_upload_pipeline.sh -f $FILE -o [output dir] -c [path to clinvar] -g [path to genes_to_phenotype.txt]; done
15. When finished all data should be uploaded to EMX2 and a new table vcf_variants should exist.
    If you can't see this table try to log out and log back in.

## Final steps
16. Navigate to: http://localhost:8080/gendecs/tables/#/Patients
17. Click on a patient and try GenDecS!
