// Les villes des zmanim : data/villes.json (GeoNames, CC-BY 4.0), le même
// fichier qu'Android et le web.

import Foundation

struct Ville: Decodable, Identifiable, Hashable {
    let id: Int
    let nom: [String: String]
    let pays: String
    let latitude: Double
    let longitude: Double
    let altitude: Double
    let fuseau: String

    static let jerusalem = 281184

    func nom(en langue: String) -> String { nom[langue] ?? nom["en"] ?? nom.values.first ?? "" }

    var lieu: Lieu { Lieu(nom: nom["en"] ?? "", latitude: latitude, longitude: longitude, altitude: altitude, fuseau: fuseau) }

    var enIsrael: Bool { pays == "IL" }

    static func charge(bundle: Bundle = .main) -> [Ville] {
        struct Fichier: Decodable { let villes: [Ville] }
        guard let url = bundle.resourceURL?.appendingPathComponent("Genere/data/villes.json"),
              let data = try? Data(contentsOf: url),
              let fichier = try? JSONDecoder().decode(Fichier.self, from: data) else { return [] }
        return fichier.villes
    }
}
