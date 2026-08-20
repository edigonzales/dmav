# DMAV 1.1 test fixture

This directory contains the DMAV 1.1 reference material used by the IOX characterization tests.

## Model

`DMAVTYM_Alles_V1_1.ili` is the current umbrella model used for the refactoring baseline. The checked-in version declares:

- model: `DMAVTYM_Alles_V1_1`
- version: `2026-01-31`
- official model repository: `https://models.geo.admin.ch/V_D/`

Source: `https://models.geo.admin.ch/V_D/DMAVTYM_Alles_V1_1.ili`

## Reduced transfer fixture

`DMAVTYM_Alles_V1_1.reduced.xtf` is deliberately small. It is based on the official DMAV 1.1 test dataset:

`https://www.cadastre-manual.admin.ch/dam/de/sd-web/hDZcCYI1qjZD/DMAV_Version_1_1.zip`

The source transfer is `DMAVTYM_Alles_V1_1.xtf`. The selected HFP3 subgraph was already extracted and validator-checked in `edigonzales/ilitransformer`; this fixture reduces that extract further while preserving the source identifiers and values needed for the dmav refactoring tests.

Preserved from the official dataset:

- basket type `DMAV_FixpunkteAVKategorie3_V1_1.FixpunkteAVKategorie3`
- BID `1331ee4d-9b33-466e-98a1-82eb52ed9c82`
- HFP3Nachfuehrung TID `34776b74-5962-43db-aa7d-d91e32009943`
- HFP3 TID `891fbd78-c4c4-4f18-b222-4ab8e43ed9a5`
- `Entstehung` reference from the HFP3 object to the HFP3Nachfuehrung object
- representative geometry and `Textposition` BAG data

The purpose of the fixture is not broad DMAV coverage. It is a compact, deterministic safety net for basket/object identity and reference preservation before replacing the XSLT implementation with iox-ili.
