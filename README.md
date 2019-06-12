# Restkid

Lightweight API testing tool

## Screenshot


<img src="https://raw.githubusercontent.com/SimonSchubert/Restkid/master/art/macos_screen_01.png" width="700">

## Why

As a mobile developer I work with private and public apis on a daily base. Postman for example is a great tool but it is build with Electron(comes with a Xbox gamepad driver) and takes ~9 seconds on my Laptop for a cold start. I'm a big fan of performant and open source software but none of the tools I tested could fullfill that requirement. And because I use the tool everyday and I love writting Kotlin I thought it might be a good opportunity to start building my own tool for my own suites.

## Todo v1.0

- Import collections from other tools like Postman and Pawn
- Edit/Add collections within the app
- Bundle .kexe to exe/application/sh
- App Icon
- App menu in statusbar
  - About
  - Settings
  - Check for update
  - ...

#### Blocked by upstream
- ScrollView for request buttons on the left side
- TreeView to display an interactive Json response (+ and - buttons)

## Todo v1.x

- Export request to code for most common frameworks (e.g. ktor,retrofit,curl)
- Export json response to classes for most common languages (e.g. POJO)

#### Blocked by upstream
- Modular alert windows with action buttons
- Buttons with icons
- Copy and paste for TextView and TextArea with standard shortcuts

## License

> Licensed under the Apache License, Version 2.0 (the "License"); you
> may not use this file except in compliance with the License. You may
> obtain a copy of the License at
>
>    http://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
> implied. See the License for the specific language governing
> permissions and limitations under the Licen
