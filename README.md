Overview GHG Emissions Mapping Application

![AppIcon](project_icon.png)

## Overview This application maps and visualizes human and natural GHG emissions, using satellite data in concert with model-based datasets. This will enable the user to select a gas, such as CO₂ or CH₄, for example, then select a region, which may be a city, select a time period, and observe the GHG emission trend in near real time, availing of high-resolution data from the United States.
The Greenhouse Gas Center and other satellite sources give quite useful information for policy analysts, scientists, and communities on ways to address climate change.

## Version 2.0.0 Update
**The application now includes additional gases with specific data sources**:
- Ozone: Ozone Data
- Aerosols: Aerosol Data
- Isoprene: Isoprene Data
**Solved problem of found no years**
**Solved bugs and performance improvements**

## Features
This then allows a user to interactively choose a particular greenhouse gas, such as CO₂, CH₄, year, and region/city, to see the resulting emissions.
- Real Time Visualization: It generates detailed visuals in the map form of human-induced and natural GHG emissions.
- **Performance**: The upgrade from Python to Kotlin finally made the app launch in less than 2 seconds.
- **Data Integration**: Integrate different satellite-derived data products into model-based estimates to develop high-resolution complete mapping.
- **Policy and Scientific Utility**: It helps the decision-makers and scientists monitor emissions, set targets, and get insight into the sources of GHGs.

## How It Works
This application is supported with **satellite-based data** and model-based estimates from various sources but more importantly from the U.S. Greenhouse Gas Center. The following app provides an interactive map in which the user can choose:
Input/output: gases, type: CO₂ or CH₄.
- A **city** or region.
- The **time period** (year).

This would process the user's input and display the trend of the emissions, in real time, for the selected period and region on the map. The data can be used to identify hotspots of emissions, understand patterns, and compare emissions between different time periods.

## Technology Stack
- **Frontend**: Kotlin
Data Sources: US Greenhouse Gas Center, NASA satellite datasets, any open source GHG datasets
- **Mapping Library**: osmmap.


Integration of Datasets
It connects datasets through the **U.S. Greenhouse Gas Center** and a variety of open source sources. Very easy to update and fix datasets by adding new files in /data or just updating links in the codebase.

## Usage
- **Analyze Regions**: Allows one to choose any region or city of their interest and provides a GHG emission map of that area.
- **Select Gas**: Highlight either CO₂ or CH₄ emission, whichever is favored.
- **Time Slider**: This time slider below shows the trend of the emissions over the years.
- **Realtime Alerting**: A major function of this app is to detect and thereby alert the user about new methane emission events.

## Contributing
Pull requests welcome! If you'd like to help improve
1. Fork the repository.
2. Create a new branch: git checkout -b feature-branch.
3. Changing and committing: git commit -m "Add feature".

4. Push to branch: git push origin feature-branch.

5. Make a pull request.

## License

This project is licensed under the MIT License. See the `LICENSE` file for more information.

## Acknowledgments

Special thanks also go out to the **NASA Space Apps Challenge**-who afforded us the opportunity-and to the **U.S. Greenhouse Gas Center** for the data to back the science of our trendlines here. 
## Contact Comments and questions are welcome, and should be directed to: mohamed.ahmed@onmail.com.
- ### Additional Resources - [U.S. Greenhouse Gas Center](https://www.ghgcenter.gov) - [Kotlin Official Documentation](https://kotlinlang.org/docs/reference/)
