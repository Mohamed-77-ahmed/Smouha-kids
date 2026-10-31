package com.example.tracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The hospital's logo is shown (batch 6, R4 and R5), but nothing may say or imply that the hospital
 * endorses, approves or recommends the app (CLAUDE.md 10), and the logo always sits on white.
 */
class BrandTextTest {

    private fun xml(path: String) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/$path"))

    /** Every user-visible text of one strings file: strings and plural items. */
    private fun texts(dir: String): List<Pair<String, String>> {
        val doc = xml("$dir/strings.xml")
        val strings = doc.getElementsByTagName("string").let { n -> (0 until n.length).map { n.item(it) } }
            .map { it.attributes.getNamedItem("name").nodeValue to it.textContent }
        val plurals = doc.getElementsByTagName("plurals").let { n -> (0 until n.length).map { n.item(it) } }
            .flatMap { p ->
                val items = p.childNodes.let { c -> (0 until c.length).map { c.item(it) } }.filter { it.nodeName == "item" }
                items.map { p.attributes.getNamedItem("name").nodeValue to it.textContent }
            }
        return strings + plurals
    }

    private val claim = Regex(
        "(?i)official|approv|endors|certif|recommend|authori[sz]ed|accredit|sponsor|on behalf|" +
            "معتمد|اعتماد|رسمي|بموافقة|موافقة|موصى|يوصي|توصي|برعاية|تحت إشراف",
    )

    @Test
    fun noTextClaimsThatTheHospitalEndorsesTheApp() {
        for (dir in listOf("values", "values-ar")) {
            val all = texts(dir)
            assertTrue("$dir has texts", all.size > 100)
            val hits = all.filter { claim.containsMatchIn(it.second) }
            assertEquals("$dir: ${hits.joinToString { it.first }}", emptyList<Pair<String, String>>(), hits)
        }
    }

    @Test
    fun theClaimPatternCatchesTheWordsItIsFor() {
        // A mutation guard: the pattern must not silently match nothing.
        listOf("The official app of the hospital", "Approved by the doctors", "التطبيق الرسمي للمستشفى", "معتمد من المستشفى")
            .forEach { assertTrue(it, claim.containsMatchIn(it)) }
        assertFalse(claim.containsMatchIn("Smouha University Children's Hospital logo"))
        assertFalse(claim.containsMatchIn("شعار مستشفى سموحة الجامعي للأطفال"))
    }

    @Test
    fun theLogoTileIsWhiteInLightAndDarkMode() {
        fun colors(dir: String) = xml("$dir/colors.xml").getElementsByTagName("color").let { n -> (0 until n.length).map { n.item(it) } }
            .associate { it.attributes.getNamedItem("name").nodeValue to it.textContent.trim().uppercase() }
        assertEquals("#FFFFFF", colors("values")["logo_tile"])
        assertFalse("no dark value: the logo is drawn on white", colors("values-night").containsKey("logo_tile"))
    }
}
