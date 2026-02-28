Overview GHG Emissions Mapping Application

![AppIcon](project_icon.png)

## Overview 
This application maps and visualizes human and natural GHG emissions, using satellite data in concert with model-based datasets. This will enable the user to select a gas, such as CO₂ or CH₄, for example, then select a region, which may be a city, select a time period, and observe the GHG emission trend in near dates. The Greenhouse Gas Center and other satellite sources give quite useful information for policy analysts, scientists, and communities on ways to address climate change.

<p align="center">
  <img src="App%20Images/Screenshot_20241008-165338.png" width="30%" />
  <img src="App%20Images/Screenshot_20241008-165524.png" width="30%" />
  <img src="App%20Images/Screenshot_20241008-165659.png" width="30%" />
</p>
## Version 2.0.0 Update
**The application now includes additional gases with specific data sources**:
- Ozone: Ozone Data
- Aerosols: Aerosol Data
- Isoprene: Isoprene Data


## How It Works
This application is supported with **satellite-based data** and model-based estimates from the U.S. Greenhouse Gas Center. The app provides an interactive map in which the user can choose:
- An **Input**: Any avaliable gas type.
- A **city** or region.
- The **time period** (year).

This would process the user's input and display the trend of the emissions, in real time, for the selected period and region on the map. The data can be used to identify hotspots of emissions, understand patterns, and compare emissions between different time periods.

## Technology Used
- Data Sources: US Greenhouse Gas Center, NASA satellite datasets, any open source GHG datasets (the data is parsed through web requests).
- **Mapping Library**: osmmap (OpenStreetMap).

## Usage
- **Analyze Regions**: Allows one to choose any region or city of their interest and provides a GHG emission map of that area.
- **Select Gas**: Highlight gases emissions, whichever is favored.
- **Time Slider**: This time slider below shows the trend of the emissions over the years.


## Acknowledgments

Thanks to **NASA Space Apps Challenge** Assiut branch for affording us the opportunity. 

- ### Additional Resources - [U.S. Greenhouse Gas Center](https://www.ghgcenter.gov)