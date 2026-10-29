// Les chaînes de l'interface : les strings.xml d'Android, lus par la version
// web (data/strings.<langue>.json) et repris ici sans retraduction.

import Foundation

struct Traduction {
    static let langues: [(code: String, nom: String)] = [("he", "עברית"), ("en", "English"), ("fr", "Français")]

    let langue: String
    private let chaines: [String: String]

    private struct Table: Decodable { let chaines: [String: String] }

    init(langue: String, bundle: Bundle = .main) {
        self.langue = langue
        let url = bundle.resourceURL?.appendingPathComponent("Genere/data/strings.\(langue).json")
        let table = url.flatMap { try? Data(contentsOf: $0) }.flatMap { try? JSONDecoder().decode(Table.self, from: $0) }
        chaines = table?.chaines ?? [:]
    }

    /// La langue choisie, sinon celle du téléphone si elle est connue, sinon le français.
    static func initiale(choisie: String?, telephone: [String] = Locale.preferredLanguages) -> String {
        let connues = langues.map(\.code)
        let normalise = { (l: String) -> String in
            let c = String(l.lowercased().prefix(2))
            return c == "iw" ? "he" : c
        }
        if let choisie, connues.contains(normalise(choisie)) { return normalise(choisie) }
        return telephone.map(normalise).first(where: { connues.contains($0) }) ?? "fr"
    }

    /// La chaîne, ou sa clé si elle manque : un trou se voit plutôt qu'il ne plante.
    func t(_ nom: String, _ args: String...) -> String {
        guard let modele = chaines[nom] else { return nom }
        return args.isEmpty ? modele : Traduction.formate(modele, args)
    }

    /// Le String.format d'Android, réduit à ce que les chaînes emploient : %s %d %1$s %2$02d %%.
    static func formate(_ modele: String, _ args: [String]) -> String {
        guard let regex = try? NSRegularExpression(pattern: "%(?:(\\d+)\\$)?(0?\\d+)?([sd%])") else { return modele }
        let ns = modele as NSString
        var resultat = ""
        var position = 0
        var suivant = 0
        for m in regex.matches(in: modele, range: NSRange(location: 0, length: ns.length)) {
            resultat += ns.substring(with: NSRange(location: position, length: m.range.location - position))
            position = m.range.location + m.range.length
            let type = ns.substring(with: m.range(at: 3))
            if type == "%" { resultat += "%"; continue }
            let index: Int
            if m.range(at: 1).location != NSNotFound {
                index = (Int(ns.substring(with: m.range(at: 1))) ?? 1) - 1
            } else {
                index = suivant
                suivant += 1
            }
            var texte = index < args.count ? args[index] : ""
            if type == "d", m.range(at: 2).location != NSNotFound {
                let largeur = ns.substring(with: m.range(at: 2))
                let n = Int(largeur) ?? 0
                let remplissage: Character = largeur.hasPrefix("0") ? "0" : " "
                if texte.count < n { texte = String(repeating: remplissage, count: n - texte.count) + texte }
            }
            resultat += texte
        }
        resultat += ns.substring(from: position)
        return resultat
    }
}
