package on.emission.maps.data

import on.emission.maps.data.model.MapDataPoint

interface CustomParser {
    fun parse(data: String): Result<List<MapDataPoint>>
}
