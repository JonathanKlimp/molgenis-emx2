# Catalogue overview
Mission of the catalogue is to facilitate multi-center pooled analysis, e.g. using a central analysis site or using federated analysis engine such as DataSHIELD. 
Federated analysis allows researchers to carry out analysis over data in multiple cohorts a) without having access to individual data and b) without individual data needing to leave the cohort. Federated analysis allows GDPR-proof research.

![Figure 1: Federated analysis](img/cat_federated-analysis.png)

To **find** the data we developed a [catalogue](https://data-catalogue.molgeniscloud.org/catalogue/catalogue/#/variable-explorer/). Here you can find the metadata of the cohorts and the common data model. 

To **access** the data we recommend MOLGENIS Armadillo. This is a platform which uses [DataSHIELD](https://datashield.org/) to perform the analysis.


We distinguish three roles that might use MOLGENIS Catalogue and MOLGENIS Armadillo:

|     |     | [Researcher](cat_researcher.md) | [Cohort data manager](cat_cohort-data-manager.md) | [Network data manager](cat_network-data-manager.md) |
| --- | --- | --- | --- | --- |
| Catalogue | Data harmonisation | | [X](cat_cohort-data-manager.md#data-harmonisation) | |
| Catalogue | Cohort metadata description | | [X](cat_cohort-data-manager.md#define-cohort-metadata) | |
| Catalogue | Describe and upload common data model | | | [X](cat_network-data-manager.md#define-network-metadata) |
| Catalogue | Find (harmonised) variables | [X](cat_researcher.md#find-variables) | | |
| Catalogue | Find harmonisation specifications | [X](cat_researcher.md#find-harmonisation-details) | [X](cat_cohort-data-manager.md#define-harmonisations) | |
| Catalogue | Request access | | [X](cat_cohort-data-manager.md#request-access-catalogue) | [X](cat_network-data-manager.md#request-access) |
| Armadillo | Initial data upload | | [X](cat_cohort-data-manager.md#initial-data-upload) | |
| Armadillo | Request access | [X](cat_researcher.md#request-access) | [X](cat_cohort-data-manager.md#request-access-armadillo) | |
| Armadillo | Request permissions | [X](cat_researcher.md#request-permissions) | [X](cat_cohort-data-manager.md#assign-permissions) | |
| Armadillo | Create subsets of the data | | [X](cat_cohort-data-manager.md#create-subsets) | |
| Armadillo | Give permissions on the data | | [X](cat_cohort-data-manager.md#give-permissions-on-the-data) | |
| Armadillo | Quality control | | [X](cat_cohort-data-manager.md#quality-control) | |
| Armadillo | Use DataSHIELD | [X](cat_researcher.md#use-datashield) | | |
| Armadillo | Deploy an instance | | | [X](cat_cohort-data-manager.md#deploy-an-instance) |
| Analysis environment | Request access | [X](cat_researcher.md#request-access) | | |