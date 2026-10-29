// Le moteur de calcul : les règles de la version web (calendrier hébraïque,
// zmanim), empaquetées dans Genere/Moteur.js par tools/prepare.sh et
// exécutées par JavaScriptCore. Rien n'est réécrit en Swift : MoteurTests
// rejoue les mêmes vecteurs KosherJava que web/test.

import Foundation
import JavaScriptCore

struct JourHebreu: Decodable, Equatable {
    /// [jour, mois, année] hébraïques.
    let h: [Int]
    /// La date hébraïque écrite, « ח׳ אלול תשפ״ה ».
    let t: String
    let f: [String]
    let o: Int
    let p: String?
    let x: Bool
    let c: [String]
}

struct Zman: Decodable, Identifiable {
    let cle: String
    let methode: String
    let parametres: String
    let statut: String
    let ms: Double?
    var id: String { cle }
    var instant: Date? { ms.map { Date(timeIntervalSince1970: $0 / 1000) } }
}

struct Lieu: Codable, Equatable {
    let nom: String
    let latitude: Double
    let longitude: Double
    let altitude: Double
    let fuseau: String
}

enum MoteurErreur: Error, CustomStringConvertible {
    case introuvable
    case javascript(String)

    var description: String {
        switch self {
        case .introuvable: return "Genere/Moteur.js absent du paquet"
        case .javascript(let message): return message
        }
    }
}

final class Moteur {
    private let contexte: JSContext
    private let objet: JSValue
    let version: String

    init(bundle: Bundle = .main) throws {
        guard let url = bundle.resourceURL?.appendingPathComponent("Genere/Moteur.js"),
              let source = try? String(contentsOf: url, encoding: .utf8),
              let contexte = JSContext() else { throw MoteurErreur.introuvable }
        contexte.evaluateScript(source, withSourceURL: url)
        if let e = contexte.exception { throw MoteurErreur.javascript(e.toString()) }
        guard let objet = contexte.objectForKeyedSubscript("Moteur"), objet.isObject else {
            throw MoteurErreur.javascript("Moteur non défini")
        }
        self.contexte = contexte
        self.objet = objet
        self.version = objet.objectForKeyedSubscript("version")?.toString() ?? ""
    }

    private func appelle(_ methode: String, _ arguments: [Any]) throws -> String {
        contexte.exception = nil
        let r = objet.invokeMethod(methode, withArguments: arguments)
        if let e = contexte.exception { throw MoteurErreur.javascript(e.toString()) }
        guard let texte = r?.toString() else { throw MoteurErreur.javascript("\(methode) : pas de réponse") }
        return texte
    }

    /// Le jour hébraïque en JSON, sous la forme exacte des vecteurs KosherJava.
    func calendrierJSON(_ iso: String, enIsrael: Bool) throws -> String {
        try appelle("calendrier", [iso, enIsrael])
    }

    func calendrier(_ iso: String, enIsrael: Bool) throws -> JourHebreu {
        try JSONDecoder().decode(JourHebreu.self, from: Data(calendrierJSON(iso, enIsrael: enIsrael).utf8))
    }

    /// Les quinze zmanim du profil, puis l'allumage des bougies.
    func zmanim(_ lieu: Lieu, _ iso: String, allumage: Int = 18) throws -> [Zman] {
        let lieuJSON = String(decoding: try JSONEncoder().encode(lieu), as: UTF8.self)
        let texte = try appelle("zmanim", [lieuJSON, iso, allumage])
        return try JSONDecoder().decode([Zman].self, from: Data(texte.utf8))
    }
}
