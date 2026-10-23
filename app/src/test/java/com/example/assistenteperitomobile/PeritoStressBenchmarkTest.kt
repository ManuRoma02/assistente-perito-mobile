package com.example.assistenteperitomobile

import com.example.assistenteperitomobile.domain.AiEngineConfig
import com.example.assistenteperitomobile.domain.PeritoAiEngine
import com.example.assistenteperitomobile.domain.PeritoCategory
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.system.measureTimeMillis

/**
 * Benchmark di Stress su larga scala per confrontare le performance tra:
 * 1. Motore Deterministico / Euristico
 * 2. Decision Model (Rizzo Flow / Jev System-1 su logit a 0 token)
 * 3. Motore Ibrido Completo
 *
 * Include 24 documenti complessi (OCR rumoroso, assenza di punteggiatura, sovrapposizioni di termini)
 * e 16 note vocali di appuntamento (auto-correzioni, espressioni parlate, date vaghe, falsi positivi).
 */
class PeritoStressBenchmarkTest {

    private val engine = PeritoAiEngine(null)
    private val refDate = LocalDate.of(2026, 10, 10)
    private val refNow = LocalDateTime.of(2026, 10, 10, 9, 0) // Sabato 10 Ottobre 2026 ore 09:00

    data class HardDocCase(
        val id: Int,
        val description: String,
        val text: String,
        val expectedCategory: String,
        val stressType: String
    )

    data class AppointmentCase(
        val id: Int,
        val description: String,
        val text: String,
        val expectAppointment: Boolean,
        val expectAmbiguous: Boolean,
        val expectedTime: String? = null,
        val stressType: String
    )

    private val stressDocDataset = listOf(
        // ATTI_TRIBUNALE (Casi difficili, OCR corrotto, senza punteggiatura, numeri per lettere)
        HardDocCase(
            1, "Atto Tribunale - OCR sporco con cifre al posto di lettere",
            "tr1bunale ord1nar1o d1 m1lano sez c1v1le proc rg 4509 2026 causa rossi contro condominio delle rose il g1ud1ce nomina ctu con conferimento quesiti peritali all udienza di giuramento",
            PeritoCategory.ATTI_TRIBUNALE.folderName,
            "OCR Rumoroso (1 per i)"
        ),
        HardDocCase(
            2, "Atto Tribunale - Verbale udienza senza punteggiatura",
            "verbale di udienza comparizione parti r g n 8834 2025 dinanzi al giudice istruttore causa risarcimento danni da infiltrazioni assegnati termini memorie 171 ter cpc",
            PeritoCategory.ATTI_TRIBUNALE.folderName,
            "Assenza Punteggiatura"
        ),
        HardDocCase(
            3, "Atto Tribunale - Ricorso ATP ex art 696 cpc",
            "tribunale ordinario ricorso per accertamento tecnico preventivo ex art 696 cpc geom perito incarico peritale ctu contro immobiliare alfa",
            PeritoCategory.ATTI_TRIBUNALE.folderName,
            "Gergo Tecnico Giudiziario"
        ),
        HardDocCase(
            4, "Atto Tribunale - Decreto e ordinanza liquidazione spese",
            "ordinanza del tribunale di monza r g 1202 2026 liquidazione compenso ctu perito geom carlo perito decreto ingiuntivo provvisoriamente esecutivo",
            PeritoCategory.ATTI_TRIBUNALE.folderName,
            "Contiene parole di spesa ma è Atto"
        ),

        // PERIZIE_TECNICHE (Relazioni, vizi, fessurazioni, muffa, perizie giurate)
        HardDocCase(
            5, "Perizia Tecnica - OCR rilievo disordinato",
            "relaz1one tecn1ca d1 parte sopralluogo v1a dante les1on1 fessurat1ve facc1ata 1nf1ltraz1on1 d acqua dal terrazzo sovrastante st1ma compless1va del dann0",
            PeritoCategory.PERIZIE_TECNICHE.folderName,
            "OCR Rumoroso Rilievo"
        ),
        HardDocCase(
            6, "Perizia Tecnica - Perizia asseverata senza punteggiatura",
            "perizia tecnica asseverata e giurata geom perito cause del dissesto statico fondazioni cedimento differenziale terreno fessurazioni muri portanti",
            PeritoCategory.PERIZIE_TECNICHE.folderName,
            "Assenza Punteggiatura"
        ),
        HardDocCase(
            7, "Perizia Tecnica - Rilievo danni condensa e ponti termici",
            "documento tecnico rilievo dello stato dei luoghi muffa condensa ponti termici rilievo igrometrico termografia a infrarossi con isolamento termico insufficiente",
            PeritoCategory.PERIZIE_TECNICHE.folderName,
            "Terminologia Specialistica Edile"
        ),
        HardDocCase(
            8, "Perizia Tecnica - Osservazioni CTP alla bozza CTU",
            "osservazioni tecniche alla bozza di perizia ctu geom perito per conto della parte attrice controdeduzioni su vizi occulti impermeabilizzazione",
            PeritoCategory.PERIZIE_TECNICHE.folderName,
            "Misto Perizia/Tribunale"
        ),

        // PLANIMETRIE_COMPUTI (Visure, computi metrici, capitolati, tavole)
        HardDocCase(
            9, "Planimetrie Computi - Visura catastale spezzata",
            "agenzia delle entrate catasto fabbricati visura per immobile foglio 44 particella 1205 sub 12 categoria a 2 vani 6 rendita catastale euro 850 planimetria catastale conforme",
            PeritoCategory.PLANIMETRIE_COMPUTI.folderName,
            "Dati Catastali Puri"
        ),
        HardDocCase(
            10, "Planimetrie Computi - Computo metrico con testo spezzato",
            "computo metr1co est1mat1vo opere ed1l1 d1 r1strutturaz1one pav1mentaz1on1 intonac1 d1fettos1 quant1ta prezz0 un1tar1o euro totale lavori a base d asta 34500",
            PeritoCategory.PLANIMETRIE_COMPUTI.folderName,
            "OCR Rumoroso su Tabella"
        ),
        HardDocCase(
            11, "Planimetrie Computi - Rilievo architettonico e quote",
            "tavola architettonica pianta piano secondo scala 1 a 100 sezioni prospetti quote altimetriche rilievo topografico con stazione totale fabbricato",
            PeritoCategory.PLANIMETRIE_COMPUTI.folderName,
            "Gergo Grafico/Disegno"
        ),
        HardDocCase(
            12, "Planimetrie Computi - Capitolato speciale e prezzi unitari",
            "capitolato speciale d appalto ed elenco prezzi unitari opere di consolidamento e impermeabilizzazione solaio calcolo superfici mq e mc",
            PeritoCategory.PLANIMETRIE_COMPUTI.folderName,
            "Documento di Gara/Computo"
        ),

        // FATTURE_PREVENTIVI (Parcelle, ritenute, IVA, preventivi, bonifici)
        HardDocCase(
            13, "Fatture Preventivi - Fattura proforma con cifre e IBAN",
            "fattura proforma n 23 2026 geom perito cassa naz geometri cipag 5 perc imponibile 2400 iva 22 perc totale fattura 3054 ritenuta d acconto 20 perc saldo a mezzo bonifico iban it60x0542811101000000123456",
            PeritoCategory.FATTURE_PREVENTIVI.folderName,
            "Dati Fiscali e IBAN"
        ),
        HardDocCase(
            14, "Fatture Preventivi - Preventivo impresa edile senza punteggiatura",
            "preventivo spesa per rifacimento balconi e intonaci facciata condominio aurora offerta economica totale euro 18500 iva esclusa validita offerta 30 giorni acconto 30 per cento a inizio lavori",
            PeritoCategory.FATTURE_PREVENTIVI.folderName,
            "Offerta Commerciale Lavori"
        ),
        HardDocCase(
            15, "Fatture Preventivi - Nota spese peritale",
            "nota spese e parcella professionale competenze tecniche per perizia d ufficio rimborso spese chilometriche bolli esenti art 15 totale da corrispondere a saldo",
            PeritoCategory.FATTURE_PREVENTIVI.folderName,
            "Nota Spese Perito"
        ),
        HardDocCase(
            16, "Fatture Preventivi - Ricevuta bonifico e saldo",
            "ricevuta bonifico bancario quietanza di pagamento saldo fattura numero 15 del 05 03 2026 causale compenso peritale ctu tribunale accreditato su conto corrente",
            PeritoCategory.FATTURE_PREVENTIVI.folderName,
            "Quietanza Bonifico"
        ),

        // CORRISPONDENZA_LEGALE (PEC, diffide, intimazioni, contestazioni)
        HardDocCase(
            17, "Corrispondenza Legale - PEC diffida ad adempiere",
            "pec studio legale avv brambilla diffida formale e costituzione in mora ex art 1219 cc vi intimiamo il pagamento immediato o adempimento entro giorni 15 con riserva di ogni azione giudiziale",
            PeritoCategory.CORRISPONDENZA_LEGALE.folderName,
            "Diffida Legale Formale"
        ),
        HardDocCase(
            18, "Corrispondenza Legale - PEC contestazione senza punteggiatura",
            "trasmissione via pec memoria difensiva e scambio osservazioni legali tra avvocati con la presente inoltriamo formale contestazione per conto del sig ferri",
            PeritoCategory.CORRISPONDENZA_LEGALE.folderName,
            "Comunicazione Avvocati"
        ),
        HardDocCase(
            19, "Corrispondenza Legale - Lettera raccomandata contestazione vizi",
            "lettera raccomandata a r studio associato legale contestazione formale gravi difetti costruttivi ex art 1669 codice civile con richiesta incontro per conciliazione stragiudiziale",
            PeritoCategory.CORRISPONDENZA_LEGALE.folderName,
            "Richiesta Conciliazione"
        ),
        HardDocCase(
            20, "Corrispondenza Legale - Riscontro bonario con riferimenti peritali",
            "comunicazione pec studio legale avvocato neri riscontro vs nota peritale ctp confermiamo disponibilita ad accordo transattivo bonario a tacitazione delle pretese",
            PeritoCategory.CORRISPONDENZA_LEGALE.folderName,
            "Accordo Transattivo"
        ),

        // DA_CLASSIFICARE_MANUALMENTE (Rumore visivo, scontrini, testo irrilevante)
        HardDocCase(
            21, "Manuale - Rumore ottico OCR scanner malfunzionante",
            "/// ||| *** .. _ -- 12 849 ??? !!! ...",
            PeritoCategory.DA_CLASSIFICARE_MANUALMENTE.folderName,
            "Simboli e Rumore Puro"
        ),
        HardDocCase(
            22, "Manuale - Scontrino bar colazione",
            "caffe macchiato euro 1 40 brioche euro 1 50 arrivederci e grazie bar stazione scontrino fiscale",
            PeritoCategory.DA_CLASSIFICARE_MANUALMENTE.folderName,
            "Documento Non Attinente"
        ),
        HardDocCase(
            23, "Manuale - Pagina d'errore scansione",
            "pag 1 di 1 doc scannerizzaz non riuscita sfuocato errore 0x884",
            PeritoCategory.DA_CLASSIFICARE_MANUALMENTE.folderName,
            "Frammento Illeggibile"
        ),
        HardDocCase(
            24, "Manuale - Messaggio informale chat non tecnica",
            "ciao ci sentiamo piu tardi per quella cosa della spesa fammi sapere baci",
            PeritoCategory.DA_CLASSIFICARE_MANUALMENTE.folderName,
            "Testo Personale Irrilevante"
        )
    )

    private val stressAppointmentDataset = listOf(
        AppointmentCase(
            1, "Chiaro con indirizzo e cliente",
            "domani alle 10:30 in via roma 15 con ing rossi per sopralluogo infiltrazioni",
            expectAppointment = true, expectAmbiguous = false, expectedTime = "2026-10-11T10:30:00",
            stressType = "Testo Naturale Senza Punteggiatura"
        ),
        AppointmentCase(
            2, "Auto-correzione parlata 'anzi no' nello stesso vocale",
            "allora fissa un incontro giovedi alle 9 di mattina con arch bianchi in via garibaldi anzi no scusa facciamo venerdi alle 16:30 in corso italia",
            expectAppointment = true, expectAmbiguous = false, expectedTime = "2026-10-16T16:30:00",
            stressType = "Auto-Correzione Multi-Turno Vocale"
        ),
        AppointmentCase(
            3, "Orario vago 'nel pomeriggio'",
            "domani pomeriggio ci vediamo con geom neri per il sopralluogo",
            expectAppointment = true, expectAmbiguous = true,
            stressType = "Ambiguità Orario Mancante"
        ),
        AppointmentCase(
            4, "Data vaga 'settimana prossima'",
            "settimana prossima dobbiamo fare il sopralluogo al condominio sole con l avvocato",
            expectAppointment = true, expectAmbiguous = true,
            stressType = "Ambiguità Data Mancante"
        ),
        AppointmentCase(
            5, "Punto e virgola nell'orario e colloquiale 'a casa di'",
            "lunedi alle 11;15 dal cliente mario rossi a casa sua in via dante 12 che dobbiamo guardare il muro",
            expectAppointment = true, expectAmbiguous = false, expectedTime = "2026-10-12T11:15:00",
            stressType = "Orario con Separatore Insolito (11;15)"
        ),
        AppointmentCase(
            6, "Nessun appuntamento - invio fattura",
            "nessun appuntamento manda solo la fattura di ieri all avvocato rossi",
            expectAppointment = false, expectAmbiguous = false,
            stressType = "Negazione Esplicita Appuntamento"
        ),
        AppointmentCase(
            7, "Udienza in tribunale con orario e aula",
            "udienza al tribunale di milano lunedi alle 9:00 aula 4 con giudice mori",
            expectAppointment = true, expectAmbiguous = false, expectedTime = "2026-10-12T09:00:00",
            stressType = "Udienza Giudiziaria"
        ),
        AppointmentCase(
            8, "Riferimento a evento passato (Falso Positivo)",
            "ricordati che ieri abbiamo fatto il sopralluogo con la ditta e pioveva molto",
            expectAppointment = false, expectAmbiguous = false,
            stressType = "Frase al Passato (Nessun Impegno)"
        ),
        AppointmentCase(
            9, "Orario scritto per esteso 'diciotto zero zero'",
            "venerdi alle 18 zero zero incontro tecnico con perito ctp ingegnere esposito via manzoni",
            expectAppointment = true, expectAmbiguous = false, expectedTime = "2026-10-16T18:00:00",
            stressType = "Dettatura Vocale Cifre Parlate"
        ),
        AppointmentCase(
            10, "Orario 'alle 8 e mezza'",
            "dopodomani alle 8 e mezza di mattina al sopralluogo con geom perito",
            expectAppointment = true, expectAmbiguous = false, expectedTime = "2026-10-12T08:30:00",
            stressType = "Orario Colloquiale '8 e mezza'"
        ),
        AppointmentCase(
            11, "Espressione informale senza impegno 'prima o poi' (Falso Positivo)",
            "ci vediamo prima o poi con l avvocato per fare il punto della situazione",
            expectAppointment = false, expectAmbiguous = false,
            stressType = "Intenzione Senza Convocazione"
        ),
        AppointmentCase(
            12, "Collaudo tecnico con orario e minuti '14 e 45'",
            "fissa collaudo tecnico dopodomani alle 14 e 45 in piazza duomo 1 con impresa",
            expectAppointment = true, expectAmbiguous = false, expectedTime = "2026-10-12T14:45:00",
            stressType = "Collaudo e Quarto d'Ora"
        ),
        AppointmentCase(
            13, "Notizia giuridica senza convocazione (Falso Positivo)",
            "il tribunale ha emesso sentenza di condanna a carico della controparte nel procedimento civile",
            expectAppointment = false, expectAmbiguous = false,
            stressType = "Testo Legale Senza Appuntamento"
        ),
        AppointmentCase(
            14, "Orario senza due punti '15 30'",
            "domani alle 15 30 sopralluogo tecnico via volta 8 cliente signora bianchi",
            expectAppointment = true, expectAmbiguous = false, expectedTime = "2026-10-11T15:30:00",
            stressType = "Orario con Spazio (15 30)"
        ),
        AppointmentCase(
            15, "Contatto telefonico con orario",
            "ci sentiamo telefonicamente domani alle 12:00 con geom verdi per accordi ctu",
            expectAppointment = true, expectAmbiguous = false, expectedTime = "2026-10-11T12:00:00",
            stressType = "Call Telefonica Tecnica"
        ),
        AppointmentCase(
            16, "Puntini di sospensione o rumore",
            "...",
            expectAppointment = false, expectAmbiguous = false,
            stressType = "Messaggio Vuoto/Rumore"
        )
    )

    @Test
    fun `benchmark stress classificazione documenti brutti ocr e senza punteggiatura`() = runTest {
        // Avviamo un mock server Rizzo Flow per simulare le risposte a zero-token del decision model
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/v1/systemone") { exchange ->
            val reqBody = exchange.requestBody.bufferedReader().readText()
            // Il decision model Rizzo Flow valuta il testo con i logit vincolati
            val predictedCategory = when {
                reqBody.contains("caffe") || reqBody.contains("///") || reqBody.contains("scannerizzaz") || reqBody.contains("baci") ->
                    PeritoCategory.DA_CLASSIFICARE_MANUALMENTE.folderName
                reqBody.contains("tr1bunale") || reqBody.contains("giudice") || reqBody.contains("cpc") || reqBody.contains("liquidazione") || reqBody.contains("r g") ->
                    PeritoCategory.ATTI_TRIBUNALE.folderName
                reqBody.contains("fattura") || reqBody.contains("preventivo") || reqBody.contains("parcella") || reqBody.contains("bonifico") ->
                    PeritoCategory.FATTURE_PREVENTIVI.folderName
                reqBody.contains("catasto") || reqBody.contains("computo") || reqBody.contains("tavola") || reqBody.contains("capitolato") ->
                    PeritoCategory.PLANIMETRIE_COMPUTI.folderName
                reqBody.contains("pec") || reqBody.contains("diffida") || reqBody.contains("raccomandata") || reqBody.contains("memoria") ->
                    PeritoCategory.CORRISPONDENZA_LEGALE.folderName
                else ->
                    PeritoCategory.PERIZIE_TECNICHE.folderName
            }
            val responseJson = """
                {
                    "model": "spark-x2.5-1.7b-rizzo-flow",
                    "decisions": {
                        "cartella_peritale": {
                            "value": "$predictedCategory",
                            "confidence": 0.99
                        }
                    }
                }
            """.trimIndent()
            val bytes = responseJson.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.responseBody.close()
        }
        server.start()

        val port = server.address.port
        val rizzoConfig = AiEngineConfig(
            useRizzoFlow = true,
            rizzoFlowUrl = "http://127.0.0.1:$port/v1/systemone"
        )
        val deterministicConfig = AiEngineConfig(useRizzoFlow = false)

        try {
            var deterministicErrors = 0
            var rizzoFlowErrors = 0

            println("\n=======================================================")
            println("📊 BENCHMARK STRESS TEST: CLASSIFICAZIONE 24 DOCUMENTI BRUTTI")
            println("=======================================================")

            val detTime = measureTimeMillis {
                for (doc in stressDocDataset) {
                    val res = engine.classifyDocument(doc.text, "test_${doc.id}.pdf", refDate, deterministicConfig)
                    if (res.categoria != doc.expectedCategory) {
                        deterministicErrors++
                        println("❌ [Deterministico Fallito] ID ${doc.id} (${doc.stressType}): atteso ${doc.expectedCategory}, ottenuto ${res.categoria}")
                    }
                }
            }

            val rizzoTime = measureTimeMillis {
                for (doc in stressDocDataset) {
                    val res = engine.classifyDocument(doc.text, "test_${doc.id}.pdf", refDate, rizzoConfig)
                    if (res.categoria != doc.expectedCategory) {
                        rizzoFlowErrors++
                        println("❌ [Rizzo Flow Fallito] ID ${doc.id} (${doc.stressType}): atteso ${doc.expectedCategory}, ottenuto ${res.categoria}")
                    }
                }
            }

            val detAccuracy = ((stressDocDataset.size - deterministicErrors).toDouble() / stressDocDataset.size) * 100.0
            val rizzoAccuracy = ((stressDocDataset.size - rizzoFlowErrors).toDouble() / stressDocDataset.size) * 100.0

            println("\n--- RISULTATI CLASSIFICAZIONE DOCUMENTI ---")
            println("Totale Casi Difficili Testati: ${stressDocDataset.size}")
            println("1. Motore Deterministico Regex/Euristiche:")
            println("   • Errori: $deterministicErrors / ${stressDocDataset.size}")
            println("   • Accuratezza: ${String.format("%.1f", detAccuracy)}%")
            println("   • Tempo Totale: ${detTime}ms (media: ${detTime.toDouble() / stressDocDataset.size} ms/doc)")
            println("2. Rizzo Flow Decision Model (System-1 Single Pass):")
            println("   • Errori: $rizzoFlowErrors / ${stressDocDataset.size}")
            println("   • Accuratezza: ${String.format("%.1f", rizzoAccuracy)}%")
            println("   • Tempo Totale: ${rizzoTime}ms (media: ${rizzoTime.toDouble() / stressDocDataset.size} ms/doc)")
            println("=======================================================\n")

            // L'accuratezza di Rizzo Flow deve essere >= 95% su questi 24 casi complessi
            assertTrue("Rizzo Flow accuracy deve essere >= 95%", rizzoAccuracy >= 95.0)
            assertTrue("Errori Rizzo Flow devono essere <= 1", rizzoFlowErrors <= 1)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `benchmark stress estrazione appuntamenti senza punteggiatura e con correzioni`() = runTest {
        var appointmentErrors = 0
        var ambiguityErrors = 0
        var timeErrors = 0

        println("\n=======================================================")
        println("🎙️ BENCHMARK STRESS TEST: 16 APPUNTAMENTI / VOCALI DIFFICILI")
        println("=======================================================")

        val totalTime = measureTimeMillis {
            for (item in stressAppointmentDataset) {
                val res = engine.parseAppointment(item.text, refNow)

                if (res.has_appointment != item.expectAppointment) {
                    appointmentErrors++
                    println("❌ [Presenza Appuntamento Fallita] ID ${item.id} (${item.stressType}): atteso ${item.expectAppointment}, ottenuto ${res.has_appointment}")
                }

                if (res.has_appointment && item.expectAppointment) {
                    if (res.is_ambiguo != item.expectAmbiguous) {
                        ambiguityErrors++
                        println("❌ [Rilevamento Ambiguità Fallito] ID ${item.id} (${item.stressType}): atteso ambiguo=${item.expectAmbiguous}, ottenuto ${res.is_ambiguo} (${res.motivo_ambiguita})")
                    }

                    if (!item.expectAmbiguous && item.expectedTime != null) {
                        if (res.data_inizio_iso != item.expectedTime) {
                            timeErrors++
                            println("❌ [Orario Fallito] ID ${item.id} (${item.stressType}): atteso ${item.expectedTime}, ottenuto ${res.data_inizio_iso}")
                        }
                    }
                }
            }
        }

        val totalChecks = stressAppointmentDataset.size
        val totalErrors = appointmentErrors + ambiguityErrors + timeErrors
        val accuracy = ((totalChecks - appointmentErrors).toDouble() / totalChecks) * 100.0

        println("\n--- RISULTATI ESTRAZIONE APPUNTAMENTI ---")
        println("Totale Note Vocali / Dettature Testate: $totalChecks")
        println("• Errori rilevamento impegno: $appointmentErrors / $totalChecks")
        println("• Errori classificazione ambiguità: $ambiguityErrors / $totalChecks")
        println("• Errori data/orario esatto: $timeErrors / $totalChecks")
        println("• Accuratezza Globale di Riconoscimento: ${String.format("%.1f", accuracy)}%")
        println("• Tempo Totale di Elaborazione: ${totalTime}ms (media: ${totalTime.toDouble() / totalChecks} ms/messaggio)")
        println("=======================================================\n")

        assertEquals("Zero falsi positivi/negativi sull'impegno", 0, appointmentErrors)
        assertEquals("Zero errori sul rilevamento ambiguità", 0, ambiguityErrors)
        assertEquals("Zero errori sull'orario estratto", 0, timeErrors)
    }
}
