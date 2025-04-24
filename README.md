# PINA

## Description

Protein Interaction Network Analyzer (PINA) is a Java based application designed to help biologists and researchers.

## Possible interactions:

- [X] Create Protein Network
- [X] Add Proteins to the Network
- [X] Remove Proteins from the Network
- [X] Obtain protein Hubs
- [X] Obtain Protein Communities
- [X] Save Network
- [X] Load Network
- [X] *Protein Link Prediction
- [ ] *Protein Link Prediction using Random Forest
- [X] *Obtain literature evidence
- [X] Update proteins from database
- [X] Delete proteins from database
- [X] Auto save new proteins in the database
- [X] Delete interactions from database
- [X] Delete communities from database

## Used classes description:

- Protein: stores general information about a protein
- Interaction: stores information about 2 protein interactions
- Community: Stores a protein community
- Network: stores general information about the network and it's protein components
- ResearchPaper: stores the information about a scientific paper returned
- NetworkService: Main service class
- AuditService: Used for audit service
