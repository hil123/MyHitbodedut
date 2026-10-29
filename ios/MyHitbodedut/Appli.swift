// L'état partagé par les écrans : langue, lieu des zmanim, moteur de calcul.

import Foundation

final class Appli: ObservableObject {
    @Published var langue: String {
        didSet {
            UserDefaults.standard.set(langue, forKey: "langue")
            tr = Traduction(langue: langue)
        }
    }
    @Published private(set) var tr: Traduction
    @Published var villeId: Int {
        didSet { UserDefaults.standard.set(villeId, forKey: "ville") }
    }

    let villes: [Ville]
    let moteur: Moteur?
    let erreurMoteur: String?

    init() {
        let langue = Traduction.initiale(choisie: UserDefaults.standard.string(forKey: "langue"))
        self.langue = langue
        tr = Traduction(langue: langue)
        let enregistree = UserDefaults.standard.integer(forKey: "ville")
        villeId = enregistree == 0 ? Ville.jerusalem : enregistree
        villes = Ville.charge()
        do {
            moteur = try Moteur()
            erreurMoteur = nil
        } catch {
            moteur = nil
            erreurMoteur = String(describing: error)
        }
    }

    var ville: Ville? { villes.first { $0.id == villeId } ?? villes.first { $0.id == Ville.jerusalem } }

    var droiteAGauche: Bool { langue == "he" }

    /// « AAAA-MM-JJ » du jour, dans le fuseau du lieu choisi.
    func isoAujourdhui(_ maintenant: Date = Date()) -> String {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = ville.flatMap { TimeZone(identifier: $0.fuseau) } ?? .current
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: maintenant)
    }

    /// « 06:38 » dans le fuseau du lieu.
    func heure(_ instant: Date) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = ville.flatMap { TimeZone(identifier: $0.fuseau) } ?? .current
        f.dateFormat = "HH:mm"
        return f.string(from: instant)
    }
}
