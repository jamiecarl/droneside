package com.jamiecarl.droneside.data.live

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Scrapes the dronedash HTML. AUFPV has no JSON snapshot, so this reads the same `data-*` hooks the site's
 * own dronedash-home.js depends on, which makes them the least likely part of the markup to change.
 */
object AufpvParser {

    private val eventIdInHref = Regex("""[?&]event=(\d+)""")
    private val pilotIdInHref = Regex("""/dronedash/pilot/(\d+)""")
    private val hexColor = Regex("""background:\s*(#[0-9A-Fa-f]{6})""")

    /** The member club directory at aufpv.org.au/clubs. Clubs without a "Club site" aren't on dronedash. */
    fun parseClubs(html: String, directoryUrl: String): List<LiveClub> {
        val doc = Jsoup.parse(html, directoryUrl)
        return doc.select("article.cl-card").mapNotNull { card ->
            val name = card.selectFirst("h3")?.text()?.trim().orEmpty()
            val site = card.select("a.nx-btn").firstOrNull { it.text().contains("Club site", ignoreCase = true) }
                ?.attr("abs:href")?.trimEnd('/')
            if (name.isEmpty() || site.isNullOrEmpty()) return@mapNotNull null
            LiveClub(name = name, baseUrl = site, logoUrl = card.selectFirst(".cl-plate img")?.attr("abs:src"))
        }
    }

    fun parseSnapshot(html: String): LiveSnapshot? {
        val doc = Jsoup.parse(html)
        val root = doc.selectFirst("[data-dash-root]") ?: return null
        val eventId = root.attr("data-event-id").toIntOrNull() ?: return null

        val events = doc.select(".dd-event-menu a.dd-event-item").mapNotNull { a ->
            val id = eventIdInHref.find(a.attr("href"))?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
            LiveEventRef(id, a.selectFirst(".dd-event-name")?.text()?.trim().orEmpty(), a.selectFirst(".dd-event-live") != null)
        }.distinctBy { it.id }

        val eventName = events.firstOrNull { it.id == eventId }?.name
            ?: doc.selectFirst(".dd-meet")?.text()?.removePrefix("·")?.trim()
            ?: doc.title().substringBefore(" — ").trim()

        return LiveSnapshot(
            eventId = eventId,
            eventName = eventName,
            serverNowMs = root.attr("data-server-now").toLongOrNull() ?: 0L,
            events = events,
            races = doc.select(".races-container > .race-box[data-race-id]").mapNotNull(::parseRace),
            upcoming = parseUpcoming(doc.select(".races-container > .race-box"))
        )
    }

    private fun parseRace(box: Element): LiveRace? {
        val id = box.attr("data-race-id").toIntOrNull() ?: return null
        val header = box.selectFirst(".race-header")
        val pilots = box.select(".laps-view-desktop .gt-row[data-pilot-id]").mapNotNull(::parsePilotRow)
        return LiveRace(
            id = id,
            label = header?.selectFirst("h3")?.ownText()?.trim().orEmpty(),
            title = header?.selectFirst(".race-number")?.ownText()?.trim().orEmpty(),
            state = LiveRaceState.parse(box.attr("data-race-state")),
            startMs = box.attr("data-start-ms").toLongOrNull() ?: 0L,
            isTimeTrial = box.attr("data-is-tt") == "true",
            heatType = box.attr("data-heat-type"),
            targetLaps = box.attr("data-target-laps").toIntOrNull() ?: 0,
            consecWindow = box.attr("data-consec-window").toIntOrNull() ?: 0,
            pilots = pilots,
            tracksideRef = header?.selectFirst(".race-bracket-info")?.attr("title")
                ?.substringAfter("FPVTrackside ·", "")?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    private fun parsePilotRow(row: Element): LivePilot? {
        val id = row.attr("data-pilot-id").toIntOrNull() ?: return null
        val nameCell = row.selectFirst("[data-col=name]")
        val name = nameCell?.attr("data-fav-name")?.takeIf { it.isNotBlank() }
            ?: nameCell?.selectFirst("a")?.text().orEmpty()
        val chan = row.selectFirst("[data-col=chan]")

        val laps = mutableMapOf<Int, Double>()
        row.selectFirst("[data-col=hs] [data-secs]")?.attr("data-secs")?.toDoubleOrNull()
            ?.takeIf { it > 0 }?.let { laps[0] = it }
        row.select("[data-col=laps] .lap[data-lap-cell]").forEach { pill ->
            val n = pill.attr("data-lap-cell").toIntOrNull() ?: return@forEach
            // data-secs is exact; the text also holds the sector chips on split-gate meetings.
            val secs = pill.attr("data-secs").toDoubleOrNull() ?: pill.ownText().toDoubleOrNull()
            if (n >= 1 && secs != null && secs > 0) laps[n] = secs
        }

        return LivePilot(
            id = id,
            name = name.trim(),
            channel = chan?.ownText()?.trim().orEmpty(),
            color = colorOf(chan),
            laps = laps,
            personalBest = row.selectFirst("[data-pb]")?.attr("data-value")?.toDoubleOrNull()?.takeIf { it > 0 },
            qualifying = nameCell?.selectFirst(".dd-qual-chip")?.text()?.trim()
        )
    }

    /** The "Next Races" card: one block per heat, a <strong> title and a chip per pilot. */
    private fun parseUpcoming(boxes: List<Element>): List<UpcomingRace> {
        val box = boxes.firstOrNull {
            it.selectFirst(".race-header h3")?.ownText()?.startsWith("Next", ignoreCase = true) == true
        } ?: return emptyList()
        return box.select("strong").mapNotNull { strong ->
            val block = strong.parent()?.parent() ?: return@mapNotNull null
            val pilots = block.select(".pilot-meta-chip").mapNotNull { chip ->
                val link = chip.selectFirst("a.leaderboard-pilot-link") ?: return@mapNotNull null
                val id = pilotIdInHref.find(link.attr("href"))?.groupValues?.get(1)?.toIntOrNull()
                    ?: return@mapNotNull null
                // The channel is the plain span straight after the pilot link.
                val channel = link.nextElementSibling()?.takeIf { !it.hasClass("dd-qual-chip") }?.text()?.trim().orEmpty()
                UpcomingPilot(id, link.text().trim(), channel, colorOf(chip))
            }
            UpcomingRace(strong.text().trim(), pilots)
        }
    }

    private fun colorOf(scope: Element?): String? =
        scope?.selectFirst(".channel-square")?.attr("style")?.let { hexColor.find(it)?.groupValues?.get(1) }
}
