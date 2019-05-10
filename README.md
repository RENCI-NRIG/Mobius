# Table of contents

- [Mobius](#Mobius)
  - [Component Diagram](#component)
  - [To do list](#todo)
  - [How to use or launch Mobius?](#docker)
# <a name="Mobius"></a>Mobius

Spring Boot Rest Server which has the capability to provision resources on different cloud infrastructures. In this release, Exogeni, Chameleon and Open Science Grid clouds are supported


- Design details can be found in [Design](./mobius/Readme.md)
- Interface specifications can be found in [Interface](./mobius/Interface.md)
- Code can be generated via swagger by referring to [HowToGenerateCodeFromSwagger](./mobius/HowToGenerateCodeFromSwagger.md)
## <a name="component"></a>Component Diagram
![Component Diagram](./mobius/plantuml/images/mobius.png)
## <a name="todo"></a>TODO List
- User / certificate information
  - Possible options
    - Option 1
      - Expose APIs for add/update/delete user
      - Pass user name when starting a new workflow
    - Option 2
      - POST workflow API to take username and credentials, controller url for each cloud type to be used later for that workflow
- Enable Mobius to pass HEAT Templates
- Create network to connect chameleon compute resources instead of using sharednet


## <a name="docker"></a>How to use or launch Mobius?
- Refer to [Docker](./docker/Readme.md) to launch Mobius
