// Le moteur sur iPhone donne exactement les réponses de KosherJava : les
// mêmes vecteurs que web/test/calendrier.test.mjs, rejoués par JavaScriptCore.

import XCTest
@testable import MyHitbodedut

final class MoteurTests: XCTestCase {
    private func vecteurs() throws -> [String: Any] {
        let url = try XCTUnwrap(Bundle(for: MoteurTests.self).url(forResource: "vecteurs_kosherjava", withExtension: "json"))
        return try XCTUnwrap(JSONSerialization.jsonObject(with: Data(contentsOf: url)) as? [String: Any])
    }

    func testCalendrierIdentiqueAKosherJava() throws {
        let moteur = try Moteur()
        let jours = try XCTUnwrap(try vecteurs()["jours"] as? [String: Any])
        for (cle, attendu) in jours {
            let parties = cle.split(separator: "/")
            let texte = try moteur.calendrierJSON(String(parties[0]), enIsrael: parties.count > 1)
            let obtenu = try JSONSerialization.jsonObject(with: Data(texte.utf8)) as? NSDictionary
            XCTAssertEqual(obtenu, attendu as? NSDictionary, cle)
        }
        XCTAssertEqual(jours.count, 3896)
    }

    func testZmanimIdentiquesAKosherJava() throws {
        let moteur = try Moteur()
        let lignes = try XCTUnwrap(try vecteurs()["zmanim"] as? [[String: Any]])
        // Les vecteurs écrivent la fraction de seconde seulement quand elle n'est pas nulle.
        let avecFraction = ISO8601DateFormatter()
        avecFraction.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let sansFraction = ISO8601DateFormatter()
        var compares = 0
        for r in lignes {
            let lieu = Lieu(nom: r["lieu"] as? String ?? "", latitude: r["lat"] as? Double ?? 0,
                            longitude: r["lon"] as? Double ?? 0, altitude: 0, fuseau: r["fuseau"] as? String ?? "")
            let date = try XCTUnwrap(r["date"] as? String)
            for z in try moteur.zmanim(lieu, date) {
                let ou = "\(lieu.nom) \(date) \(z.cle)"
                XCTAssertNotEqual(z.statut, "erreur", ou)
                let attendu = try XCTUnwrap(r[z.cle] as? String, ou)
                if attendu == "ABSENT" { XCTAssertEqual(z.statut, "absent", ou); continue }
                let instant = try XCTUnwrap(avecFraction.date(from: attendu) ?? sansFraction.date(from: attendu), ou)
                let obtenu = try XCTUnwrap(z.instant, ou)
                XCTAssertLessThanOrEqual(abs(obtenu.timeIntervalSince(instant)), 1, ou)
                compares += 1
            }
        }
        XCTAssertEqual(compares, 531)
    }

    func testFormatAndroid() {
        XCTAssertEqual(Traduction.formate("Série : %1$d jours sur %2$d", ["3", "7"]), "Série : 3 jours sur 7")
        XCTAssertEqual(Traduction.formate("%s et %s, 100 %%", ["a", "b"]), "a et b, 100 %")
        XCTAssertEqual(Traduction.formate("%1$02d:%2$02d", ["5", "7"]), "05:07")
    }

    func testLangueInitiale() {
        XCTAssertEqual(Traduction.initiale(choisie: nil, telephone: ["iw-IL"]), "he")
        XCTAssertEqual(Traduction.initiale(choisie: nil, telephone: ["de-DE", "en-US"]), "en")
        XCTAssertEqual(Traduction.initiale(choisie: nil, telephone: ["de-DE"]), "fr")
        XCTAssertEqual(Traduction.initiale(choisie: "en", telephone: ["he"]), "en")
    }
}
