# Mobius

Spring Boot Rest Server which has the capability to provision resources on different cloud infrastructures. In this release, Exogeni, Chameleon and Open Science Grid clouds are supported


- Design details can be found in [Design](./DESIGN.md)
- Interface specifications can be found in [Interface](./Interface.md)
- Code can be generated via swagger by referring to [HowToGenerateCodeFromSwagger](./HowToGenerateCodeFromSwagger.md)

## Block Diagram
![Component Diagram](./plantuml/images/component.png)

## Open Implementation Issues
- User / certificate information
  - Possible options
    - Option 1
      - Expose APIs for add/update/delete user
      - Pass user name when starting a new workflow
    - Option 2
      - POST workflow API to take username and credentials, controller url for each cloud type to be used later for that workflow
- Where will mobius be deployed?
  - Run on individual laptop, node on AWS or Exogeni?
- Lease time with Storage
  - Results on renewal to whole slice
  - Can multiple storage be requested with single node? If so, we need a way to distinguish them to support delete operation on them?
  - Update operation on storage cannot be supported (increase/decrease)
  - Keep - What is the expectation ?
- TODO
    - Periodically process future request
