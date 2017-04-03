/*
 * Copyright 2017 Adam Jordens
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jordens.lalas.feeding

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.apache.commons.csv.CSVRecord
import org.apache.commons.csv.CSVFormat
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.RequestParam
import java.io.StringReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference

@RestController
@RequestMapping(value = "/api/feedings")
class FeedingController @Autowired constructor(val configuration: FeedingConfigurationProperties) {
  val logger = LoggerFactory.getLogger(FeedingController::class.java)
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  val feedings: AtomicReference<List<Feeding>> = AtomicReference(emptyList())

  @GetMapping("/")
  fun all(): List<Feeding> = feedings.get()

  @GetMapping("/byDay")
  fun allByDay(@RequestParam(value = "time", required = false) time: String?,
               @RequestParam(value = "sort", required = false, defaultValue = "date") sort: String): List<FeedingAggregate> {
    val feedingsByDay: MutableMap<String, MutableList<Feeding>> = feedings.get().groupByTo(mutableMapOf()) {
      if (time != null && it.time < time) {
        LocalDate.parse(it.date, formatter).minusDays(1).format(formatter)
      } else {
        it.date
      }
    }

    val feedings = feedingsByDay.map { f ->
      FeedingAggregate(
        f.key,
        f.value.sumBy { it.milkVolumeMilliliters },
        f.value.sumBy { it.diaperTypes.size },
        f.value.sumBy { it.nursingDurationMinutes }
      )
    }

    // figure out how to make a generic comparator based off of `sort`
    when (sort) {
      "milkVolumeMilliliters" -> return feedings.sortedByDescending { it.milkVolumeMilliliters }
      "diaperCount" -> return feedings.sortedByDescending { it.diaperCount }
      "nursingDurationMinutes" -> return feedings.sortedByDescending { it.nursingDurationMinutes }
      else -> return feedings.sortedByDescending { it.date }
    }
  }

  @Scheduled(fixedRate = 300000)
  fun fetchFeedings() {
    val sourceUrl = configuration.sourceAsUrl()
    logger.info("Fetching feedings from $sourceUrl")

    val column = { record: CSVRecord, columnName: String ->
      val value = record.get(columnName)
      if (value.isNullOrEmpty()) null else value
    }

    val records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(StringReader(sourceUrl.readText()))
    feedings.set(
      records.map { record ->
        Feeding(
          column(record, "date") ?: "n/a",
          column(record, "time") ?: "n/a",
          (column(record, "nursing_duration_minutes") ?: "0").toInt(),
          column(record, "milk_type") ?: "n/a",
          (column(record, "milk_volume_ml") ?: "0").toInt(),
          column(record, "body_temperature")?.toDouble(),
          (column(record, "diaper_type") ?: "").split(","),
          column(record, "notes") ?: "n/a"
        )
      }
    )

    logger.info("Fetched ${feedings.get().size} feedings from $sourceUrl")
  }
}

data class Feeding(val date: String,
                   val time: String,
                   val nursingDurationMinutes: Int,
                   val milkType: String,
                   val milkVolumeMilliliters: Int,
                   val bodyTemperature: Double?,
                   val diaperTypes: Collection<String>,
                   val notes: String)

data class FeedingAggregate(val date: String,
                            val milkVolumeMilliliters: Int,
                            val diaperCount: Int,
                            val nursingDurationMinutes: Int)
