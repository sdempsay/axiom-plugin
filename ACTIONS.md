# ACTIONS

## 2026-08-31

- C2 (`Fixes #3`): `axiom-maven-plugin` goal `catalog` validates via axiom-model, stamps `${project.version}`, writes `target/axiom/catalog.yaml`, and attaches `classifier=agent-catalog` type `yaml`. `required=true` fails on missing/invalid catalogs. Fixture IT looks like Exceptional. Embed/harvest stay issue #4.
- C1 (`Fixes #1`, `Fixes #2`): published `schema/axiom-catalog-1.json` (JSON Schema 2020-12) and `org.dempsay.axiom:axiom-model`. `CatalogValidator` parses YAML via Jackson, fails on missing intent id / blessed symbol / snippet file, duplicate ids, and invalid regex. Exceptional example catalog is the valid fixture; primary snippet is `of`+`execute`, chain is secondary. Site reports skipped until dempsay-parent site/versions plugins align.
- Adopted hybrid TODO.md + GitHub Issues. Plugin tasks T1–T4 are issues #1–#4.
