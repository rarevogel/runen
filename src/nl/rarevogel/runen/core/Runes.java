package nl.rarevogel.runen.core;

import java.util.ArrayList;
import java.util.List;

/** The two rune rows with names, sounds, meanings, memory aids and example words. */
public final class Runes {

    public static final class Example {
        public final String latin;    // how we read it
        public final String runes;    // how it is carved
        public final String note;     // who or what it is
        public final boolean authentic;  // spelled as on a real Viking-age stone

        Example(String latin, String runes, String note, boolean authentic) {
            this.latin = latin; this.runes = runes; this.note = note; this.authentic = authentic;
        }
    }

    public static final class Rune {
        public final Track track;
        public final int index;
        public final String glyph, name, sound, meaning, memo;
        public String pron = "";      // phonemes for the audio generator
        public final List<Example> examples = new ArrayList<>();

        Rune(Track track, int index, String glyph, String name, String sound, String meaning, String memo) {
            this.track = track; this.index = index; this.glyph = glyph; this.name = name; this.sound = sound;
            this.meaning = meaning; this.memo = memo;
        }

        public String key() { return track.id + index; }
        public String audio() { return "n_" + track.id + index; }
    }

    public static final class Track {
        public final String id, title, subtitle, tab;
        public final boolean younger;
        public final int mode;
        public final List<Rune> runes = new ArrayList<>();
        public final String[] aettNames;
        public final int[] aettStart;

        Track(String id, String title, String tab, String subtitle, int mode, String[] aettNames, int[] aettStart) {
            this.id = id; this.title = title; this.tab = tab; this.subtitle = subtitle; this.mode = mode;
            this.younger = mode == Translit.YOUNGER;
            this.aettNames = aettNames; this.aettStart = aettStart;
        }

        Rune add(String glyph, String name, String sound, String meaning, String memo) {
            Rune r = new Rune(this, runes.size(), glyph, name, sound, meaning, memo);
            runes.add(r);
            return r;
        }

        public int size() { return runes.size(); }

        public int aettOf(int i) {
            int a = 0;
            for (int k = 0; k < aettStart.length; k++) if (i >= aettStart[k]) a = k;
            return a;
        }
    }

    public static final Track ELDER = new Track("O", "Oude futhark", "Oud", "24 runen, ca. 150-750 n.Chr.", Translit.ELDER,
            new String[]{"Freys ætt", "Hagals ætt", "Tyrs ætt"}, new int[]{0, 8, 16});
    public static final Track YOUNGER = new Track("J", "Jongere futhark", "Viking", "16 runen van de vikingtijd", Translit.YOUNGER,
            new String[]{"Freys ætt", "Hagals ætt", "Tyrs ætt"}, new int[]{0, 6, 11});
    public static final Track FUTHORC = new Track("A", "Futhorc", "Futhorc", "28 runen van Friezen en Angelsaksen", Translit.FUTHORC,
            new String[]{"Eerste ætt", "Tweede ætt", "Derde ætt", "Extra runen"}, new int[]{0, 8, 16, 24});
    public static final Track[] TRACKS = {ELDER, YOUNGER, FUTHORC};

    static void ex(Rune r, String latin, String note) {
        r.examples.add(new Example(latin, Translit.toRunes(latin, r.track.mode, false), note, false));
    }

    static void old(Rune r, String latin, String runes, String note) {
        r.examples.add(new Example(latin, runes, note, true));
    }

    static {
        Track t = ELDER;
        Rune r;
        r = t.add("ᚠ", "fehu", "f", "vee, rijkdom", "Twee takken schuin omhoog: de horens van een koe. Vee was rijkdom.");
        ex(r, "Floki", "Flóki, de viking die IJsland zijn naam gaf");
        ex(r, "Friesland", "kustgebied waar vikingen vaak aan land kwamen");
        r = t.add("ᚢ", "uruz", "u (oe)", "oeros, de wilde stier", "De gebogen rug van een oeros met de kop naar beneden.");
        ex(r, "Uppsala", "heilige plaats en koningszetel in Zweden");
        ex(r, "Ulf", "veelvoorkomende vikingnaam: 'wolf'");
        r = t.add("ᚦ", "thurisaz", "th", "reus", "Een doorn op een stok. Thor sloeg reuzen met zijn hamer.");
        ex(r, "Thor", "god van de donder");
        ex(r, "Thorshavn", "hoofdstad van de Faeröer: 'haven van Thor'");
        r = t.add("ᚨ", "ansuz", "a", "god (een van de Asen)", "Twee takken die naar beneden hangen, als bij een A van Asen.");
        ex(r, "Asgard", "de wereld van de goden");
        ex(r, "Arnhem", "stad aan de Rijn");
        r = t.add("ᚱ", "raido", "r", "rit, reis", "Lijkt op onze R. Op reis!");
        ex(r, "Rorik", "Rorik van Dorestad, viking die over Friesland heerste");
        ex(r, "Ribe", "oudste stad van Denemarken, een vikingmarkt");
        r = t.add("ᚲ", "kaunan", "k", "zweer, of: fakkel", "Een open snavel: < zoals bij een k die 'klik' zegt.");
        ex(r, "Knut", "Knut de Grote, koning van Denemarken en Engeland");
        ex(r, "Kattegat", "zee tussen Denemarken en Zweden");
        r = t.add("ᚷ", "gebo", "g", "gift, geschenk", "Een X: iets dat je van de een aan de ander geeft.");
        ex(r, "Godfried", "vikingkoning die rond 880 over Friesland heerste");
        ex(r, "Gotland", "Zweeds eiland vol beeldstenen");
        r = t.add("ᚹ", "wunjo", "w", "vreugde", "Een wapperend vlaggetje: een W van feest.");
        ex(r, "Wodan", "Germaanse naam van Odin, de oppergod");
        ex(r, "Wieringen", "hier zijn vikingschatten van zilver gevonden");

        r = t.add("ᚺ", "hagalaz", "h", "hagel", "Een H met een schuine dwarsbalk, schuin als vallende hagel.");
        ex(r, "Harald", "Harald Blauwtand, koning van Denemarken");
        ex(r, "Hedeby", "grote vikinghandelsplaats bij Sleeswijk");
        r = t.add("ᚾ", "naudiz", "n", "nood", "Een stok met een streep erdoor: in nood kruis je je armen.");
        ex(r, "Njord", "god van de zee en de wind");
        ex(r, "Noorwegen", "land van de fjorden");
        r = t.add("ᛁ", "isaz", "i", "ijs", "Een rechte ijspegel: een i zonder punt.");
        ex(r, "Ivar", "Ivar de Beenloze, zoon van Ragnar");
        ex(r, "Ingrid", "Scandinavische meisjesnaam");
        r = t.add("ᛃ", "jera", "j", "jaar, oogst", "Twee haken die om elkaar draaien, zoals de seizoenen van het jaar.");
        ex(r, "Jelling", "koningsplaats in Denemarken met beroemde runenstenen");
        ex(r, "Jutland", "het vasteland van Denemarken");
        r = t.add("ᛇ", "eihwaz", "ei", "taxus (ijbenboom)", "Een lange stam met een haak boven en onder. De klank is onzeker; we gebruiken hem hier voor 'ei'.");
        ex(r, "Einar", "vikingnaam: 'eenzame strijder'");
        ex(r, "Leif", "Leif Eriksson zeilde rond 1000 naar Amerika");
        r = t.add("ᛈ", "perthro", "p", "onbekend, misschien een dobbelbeker", "Een beker op zijn kant, met de opening naar rechts: p van potje.");
        ex(r, "Paris", "Parijs: vikingen vielen de stad aan in 845");
        ex(r, "Poppo", "monnik die Harald Blauwtand tot het christendom bekeerde");
        r = t.add("ᛉ", "algiz", "z", "eland, bescherming", "Een eland met een gewei, of een hand die afweert.");
        ex(r, "Zeeland", "eilanden in de Schelde-delta");
        ex(r, "Zwolle", "Hanzestad aan het Zwarte Water");
        r = t.add("ᛊ", "sowilo", "s", "zon", "Een bliksem of zonnestraal, met de vorm van een S.");
        ex(r, "Sigurd", "held die de draak Fafnir versloeg");
        ex(r, "Skagen", "noordpunt van Denemarken");

        r = t.add("ᛏ", "tiwaz", "t", "de god Tyr", "Een pijl omhoog: T van Tyr, de god van de strijd.");
        ex(r, "Tyr", "eenhandige god van recht en strijd");
        ex(r, "Tiel", "stad aan de Waal, in 1007 door vikingen geplunderd");
        r = t.add("ᛒ", "berkano", "b", "berk", "Lijkt op onze B.");
        ex(r, "Birka", "vikingstad op een eiland in het Mälarmeer");
        ex(r, "Bjorn", "vikingnaam: 'beer'");
        r = t.add("ᛖ", "ehwaz", "e", "paard", "Lijkt op een M, maar is een e. Twee paardenbenen met een zadel.");
        ex(r, "Erik", "Erik de Rode stichtte de eerste nederzetting op Groenland");
        ex(r, "Egil", "Egil Skallagrimsson, dichter en viking");
        r = t.add("ᛗ", "mannaz", "m", "mens", "Twee mensen die elkaar vasthouden: M van mens.");
        ex(r, "Midgard", "de wereld van de mensen");
        ex(r, "Medemblik", "oude handelsplaats in West-Friesland");
        r = t.add("ᛚ", "laguz", "l", "water, meer", "Een hengel boven het water: L van lijn.");
        ex(r, "Loki", "de listige god");
        ex(r, "Leiden", "stad aan de Oude Rijn");
        r = t.add("ᛜ", "ingwaz", "ng", "de god Ing", "Een kleine ruit: de ng in koning.");
        ex(r, "Ingvar", "Ingvar de Verrereiziger, op tientallen runenstenen genoemd");
        ex(r, "Groningen", "Groninger stad in het noorden");
        r = t.add("ᛞ", "dagaz", "d", "dag", "Een zandloper: van dag naar nacht en terug.");
        ex(r, "Dorestad", "handelsstad bij Wijk bij Duurstede, in de 9e eeuw door vikingen geplunderd");
        ex(r, "Dublin", "door vikingen gestichte stad in Ierland");
        r = t.add("ᛟ", "othala", "o", "erfgoed, eigen grond", "Een huis met een puntdak en twee benen: o van oord.");
        ex(r, "Odin", "oppergod, vader van Thor");
        ex(r, "Oslo", "hoofdstad van Noorwegen");

        t = YOUNGER;
        r = t.add("ᚠ", "fé", "f, v", "vee, rijkdom", "Dezelfde rune als in de oude futhark: de horens van een koe.");
        ex(r, "Floki", "Flóki, de viking die IJsland zijn naam gaf");
        old(r, "Ulf (ulfuR)", "ᚢᛚᚠᚢᛦ", "zo staat Ulf op de schedel van Ribe, ca. 725");
        r = t.add("ᚢ", "úr", "u, o, v, w", "motregen, of: ijzerslak", "Dezelfde vorm als in de oude futhark, maar nu voor u, o, v én w.");
        ex(r, "Uppsala", "heilige plaats en koningszetel in Zweden");
        old(r, "Odin (uþin)", "ᚢᚦᛁᚾ", "zo staat Odin op de schedel van Ribe");
        r = t.add("ᚦ", "þurs", "th, dh", "reus", "Een doorn op een stok, net als in de oude futhark.");
        ex(r, "Thor", "god van de donder");
        old(r, "Thor (þur)", "ᚦᚢᚱ", "'Thor wijde deze runen', op de steen van Glavendrup");
        r = t.add("ᚬ", "óss", "o, nasale a", "god, of: riviermonding", "Lijkt op ᚨ, maar de takken steken door de stam heen.");
        ex(r, "Oslo", "hoofdstad van Noorwegen");
        old(r, "Thyra (þąurui)", "ᚦᚬᚢᚱᚢᛁ", "koningin Thyra op de grote steen van Jelling");
        r = t.add("ᚱ", "reið", "r", "rit, reis", "Lijkt op onze R, net als in de oude futhark.");
        ex(r, "Ribe", "oudste stad van Denemarken, een vikingmarkt");
        old(r, "Harald (haraltr)", "ᚼᛅᚱᛅᛚᛏᚱ", "zo schreef Harald Blauwtand zijn naam in Jelling");
        r = t.add("ᚴ", "kaun", "k, g", "zweer", "Een stok met één tak schuin omhoog. Hij staat voor zowel k als g.");
        ex(r, "Gotland", "Zweeds eiland vol beeldstenen");
        old(r, "Gorm (kurm)", "ᚴᚢᚱᛘ", "koning Gorm de Oude op de grote steen van Jelling");
        r = t.add("ᚼ", "hagall", "h", "hagel", "Een sterretje van twee schuine strepen door de stam: hagelkorrels.");
        ex(r, "Hedeby", "grote vikinghandelsplaats bij Sleeswijk");
        old(r, "Harald (haraltr)", "ᚼᛅᚱᛅᛚᛏᚱ", "op de grote steen van Jelling, ca. 965");
        r = t.add("ᚾ", "nauðr", "n", "nood", "De streep loopt van linksboven naar rechtsonder. Let op het verschil met ᛅ!");
        ex(r, "Njord", "god van de zee en de wind");
        old(r, "Noorwegen (nuruiak)", "ᚾᚢᚱᚢᛁᛅᚴ", "Harald noemt Noorwegen op de grote steen van Jelling");
        r = t.add("ᛁ", "íss", "i, e, j", "ijs", "Een rechte ijspegel. In de vikingtijd ook voor e en j.");
        ex(r, "Ivar", "Ivar de Beenloze, zoon van Ragnar");
        old(r, "christenen (kristną)", "ᚴᚱᛁᛋᛏᚾᚬ", "Harald 'maakte de Denen christen', grote steen van Jelling");
        r = t.add("ᛅ", "ár", "a, æ", "jaar, goede oogst", "De streep loopt van linksonder naar rechtsboven: andersom dan bij ᚾ.");
        ex(r, "Asgard", "de wereld van de goden");
        old(r, "Denemarken (tanmaurk)", "ᛏᛅᚾᛘᛅᚢᚱᚴ", "Denemarken op de grote steen van Jelling");
        r = t.add("ᛋ", "sól", "s, z", "zon", "Een zigzag: een korte streep, een schuine en weer een korte streep. De ᛊ van de oude futhark in een nieuwe vorm.");
        ex(r, "Sigurd", "held die de draak Fafnir versloeg");
        old(r, "zijn (sina)", "ᛋᛁᚾᛅ", "'zijn vrouw', op de kleine steen van Jelling");
        r = t.add("ᛏ", "týr", "t, d", "de god Tyr", "Een pijl omhoog. In de vikingtijd ook voor de d.");
        ex(r, "Dorestad", "handelsstad bij Wijk bij Duurstede, in de 9e eeuw door vikingen geplunderd");
        old(r, "Denemarken (tanmaurk)", "ᛏᛅᚾᛘᛅᚢᚱᚴ", "met ᛏ voor de D, grote steen van Jelling");
        r = t.add("ᛒ", "bjarkan", "b, p", "berk", "Lijkt op onze B. Ook voor de p, want ᛈ bestond niet meer.");
        ex(r, "Birka", "vikingstad op een eiland in het Mälarmeer");
        old(r, "gedenkteken (kubl)", "ᚴᚢᛒᛚ", "'Gorm maakte dit gedenkteken', kleine steen van Jelling");
        r = t.add("ᛘ", "maðr", "m", "mens", "Een stam met twee armen omhoog: een mens die zwaait.");
        ex(r, "Midgard", "de wereld van de mensen");
        old(r, "Gorm (kurmR)", "ᚴᚢᚱᛘᛦ", "koning Gorm op de kleine steen van Jelling");
        r = t.add("ᛚ", "lögr", "l", "water, zee", "Een hengel boven het water, net als in de oude futhark.");
        ex(r, "Loki", "de listige god");
        old(r, "Harald (haraltr)", "ᚼᛅᚱᛅᛚᛏᚱ", "op de grote steen van Jelling");
        r = t.add("ᛦ", "yr", "R (eind-r)", "taxus", "Een mens die op zijn hoofd staat: ᛘ ondersteboven. Vaak de r aan het eind van een naam.");
        old(r, "Gorm (kurmR)", "ᚴᚢᚱᛘᛦ", "koning Gorm op de kleine steen van Jelling");
        old(r, "Denemarken (tanmarkaR)", "ᛏᛅᚾᛘᛅᚱᚴᛅᛦ", "'Denemarkens sieraad', over koningin Thyra");
        old(r, "Ulf (ulfuR)", "ᚢᛚᚠᚢᛦ", "op de schedel van Ribe");
    }

    /** Short sound label for a rune character, also for the dotted runes: "i/e/j", "th/dh". */
    public static String soundLabel(Track t, char c) {
        int d = Translit.DOTTED_RUNES.indexOf(c);
        if (d >= 0) return String.valueOf(Translit.DOTTED_LATIN.charAt(d));
        Rune r = byGlyph(t, c);
        if (r == null) return String.valueOf(c);
        String snd = r.sound.replace(", ", "/");
        int sp = snd.indexOf(' ');
        return sp > 0 ? snd.substring(0, sp) : snd;
    }

    static void futhorc() {
        Track t = FUTHORC;
        Rune r;
        r = t.add("ᚠ", "feoh", "f", "vee, rijkdom", "Dezelfde rune als in de oude futhark: de horens van een koe.");
        ex(r, "Finn", "Friese koning uit het verhaal van Beowulf");
        ex(r, "Friesland", "de Friezen gebruikten dezelfde runen als de Angelsaksen");
        r = t.add("ᚢ", "ur", "u", "oeros", "De gebogen rug van een oeros, net als in de oude futhark.");
        ex(r, "Utrecht", "de Engelse monnik Willibrord werd hier de eerste bisschop");
        r = t.add("ᚦ", "thorn", "th", "doorn", "Een doorn op een stok. Het Engelse woord thorn betekent nog steeds doorn.");
        ex(r, "Thames", "de rivier waarin het mes van Beagnoth werd gevonden");
        r = t.add("ᚩ", "os", "o", "mond, of: god", "Een stam met twee takken die naar beneden hangen. De a van de oude futhark werd hier een o.");
        ex(r, "Offa", "koning van Mercia, bekend van de grote wal Offa's Dyke");
        r = t.add("ᚱ", "rad", "r", "rit", "Lijkt op onze R, net als in de andere runenrijen.");
        ex(r, "Radboud", "koning van de Friezen rond het jaar 700");
        r = t.add("ᚳ", "cen", "k, c", "fakkel", "Een stok met een haak bovenaan: de vlam van een fakkel.");
        ex(r, "Canterbury", "hier begon in 597 de bekering van de Angelsaksen");
        r = t.add("ᚷ", "gyfu", "g", "gift", "Een X: iets wat je aan een ander geeft.");
        ex(r, "Groningen", "provincie waar Friese runen zijn gevonden, zoals in Westeremden");
        r = t.add("ᚹ", "wynn", "w", "vreugde", "Een wapperend vlaggetje. Deze rune werd later ook als letter in gewoon Oudengels schrift gebruikt.");
        ex(r, "Willibrord", "Engelse monnik die de Friezen kerstende");

        r = t.add("ᚻ", "hægl", "h", "hagel", "Een H met twee schuine dwarsstreepjes; in de oude futhark was het er één.");
        ex(r, "Hengist", "legendarische aanvoerder van de eerste Angelsaksen in Engeland");
        r = t.add("ᚾ", "nyd", "n", "nood", "Een stok met een streep erdoor, net als in de oude futhark.");
        ex(r, "Northumbria", "Angelsaksisch koninkrijk in het noorden van Engeland");
        r = t.add("ᛁ", "is", "i", "ijs", "Een rechte ijspegel.");
        ex(r, "Ipswich", "een van de oudste Angelsaksische handelssteden");
        r = t.add("ᛄ", "ger", "j", "jaar, oogst", "Een stok met een ruitje: het rad van het jaar.");
        ex(r, "Jarrow", "klooster waar de geleerde monnik Beda leefde");
        r = t.add("ᛇ", "eoh", "eo", "taxus", "Lange stam met een haak boven en onder, net als in de oude futhark. Hier voor de eo-klank.");
        ex(r, "Beowulf", "held van het beroemdste Oudengelse gedicht");
        ex(r, "Eoforwic", "de Angelsaksische naam van York");
        r = t.add("ᛈ", "peorð", "p", "onbekend", "Een beker op zijn kant, net als in de oude futhark.");
        ex(r, "Penda", "de laatste heidense koning van Mercia");
        r = t.add("ᛉ", "eolh", "x", "elandzegge, een soort riet", "Hetzelfde teken als ᛉ in de oude futhark, maar hier voor de x.");
        ex(r, "Wessex", "koninkrijk van Alfred de Grote");
        ex(r, "Essex", "koninkrijk van de Oost-Saksen");
        r = t.add("ᛋ", "sigel", "s", "zon", "Een bliksemflits: de S van sigel, de zon.");
        ex(r, "Sutton Hoo", "beroemd scheepsgraf van een Angelsaksische koning");

        r = t.add("ᛏ", "tir", "t", "de god Tiw, of: roem", "Een pijl omhoog, net als in de oude futhark.");
        ex(r, "Tamworth", "hoofdplaats van het koninkrijk Mercia");
        r = t.add("ᛒ", "beorc", "b", "berk", "Lijkt op onze B.");
        ex(r, "Beda", "monnik en geschiedschrijver, rond 700");
        old(r, "Beagnoth (beagnoþ)", "ᛒᛠᚷᚾᚩᚦ", "de naam op het mes uit de Theems");
        r = t.add("ᛖ", "eh", "e", "paard", "Lijkt op een M maar is een e, net als in de oude futhark.");
        ex(r, "Edwin", "koning van Northumbria, in 627 gedoopt");
        r = t.add("ᛗ", "mann", "m", "mens", "Twee mensen die elkaar vasthouden: M van mens.");
        ex(r, "Mercia", "Angelsaksisch koninkrijk in Midden-Engeland");
        r = t.add("ᛚ", "lagu", "l", "water, zee", "Een hengel boven het water.");
        ex(r, "Lindisfarne", "klooster dat in 793 door vikingen werd overvallen");
        r = t.add("ᛝ", "ing", "ng", "de held Ing", "Twee ruitjes boven elkaar: de ng.");
        ex(r, "England", "het land van de Angelen");
        r = t.add("ᛟ", "eþel", "œ (eu)", "erfgoed, vaderland", "Dezelfde vorm als ᛟ in de oude futhark, maar de klank is nu œ, zoals de eu in neus.");
        ex(r, "Europa", "ons werelddeel: de eu klinkt als de œ");
        r = t.add("ᛞ", "dæg", "d", "dag", "Een zandloper: van dag naar nacht.");
        ex(r, "Dokkum", "hier werd de Engelse missionaris Bonifatius in 754 vermoord");

        r = t.add("ᚪ", "ac", "a", "eik", "De eik. Vergelijk hem goed met ᚩ (o) en ᚫ (æ): ze lijken op elkaar.");
        ex(r, "Alfred", "Alfred de Grote, koning van Wessex, versloeg de vikingen");
        r = t.add("ᚫ", "æsc", "æ", "es (de boom)", "De es. Daarom heet de letter æ in het Engels nog steeds 'ash'.");
        ex(r, "Aethelstan", "de eerste koning van heel Engeland, vanaf 927");
        r = t.add("ᚣ", "yr", "y", "boog, of: wapen", "Een ᚢ met een streep erin: de y.");
        ex(r, "York", "Angelsaksisch Eoforwic, later de vikingstad Jorvik");
        r = t.add("ᛠ", "ear", "ea", "aarde, graf", "Een pijl omhoog met vertakte poten: de ea.");
        ex(r, "Eadmund", "koning van East Anglia, in 869 door vikingen gedood");
    }

    static final String[] PRON_O = {"'fexu", "'uruz", "'Turisaz", "'ansuz", "'raiDo:", "'kaunan", "'gevo:", "'wunjo:",
            "'xaQalaz", "'nauDiz", "'i:saz", "'je:ra", "'eixwaz", "'perTro:", "'alQiz", "'so:wilo:",
            "'ti:waz", "'berkano:", "'exwaz", "'mannaz", "'laQuz", "'iNgwaz", "'daQaz", "'o:Tala"};
    static final String[] PRON_J = {"'fe:", "'u:r", "'Turs", "'o:ss", "'reiD", "'kaun", "'haQall", "'nauDr",
            "'i:ss", "'a:r", "'so:l", "'ty:r", "'bjarkan", "'maDr", "'lWQr", "'y:r"};
    static final String[] PRON_A = {"'feox", "'u:r", "'Torn", "'o:s", "'ra:d", "'tSe:n", "'jyvu", "'wynn",
            "'hEjl", "'ny:d", "'i:s", "'jEar", "'e:ox", "'peorT", "'eolx", "'sijel",
            "'ti:r", "'beork", "'ex", "'mann", "'laQu", "'iNg", "'e:Del", "'dEj",
            "'a:k", "'ES", "'y:r", "'Ear"};

    static {
        futhorc();
        for (int i = 0; i < ELDER.size(); i++) ELDER.runes.get(i).pron = PRON_O[i];
        for (int i = 0; i < YOUNGER.size(); i++) YOUNGER.runes.get(i).pron = PRON_J[i];
        for (int i = 0; i < FUTHORC.size(); i++) FUTHORC.runes.get(i).pron = PRON_A[i];
    }

    public static Track track(String id) { return id.equals("J") ? YOUNGER : id.equals("A") ? FUTHORC : ELDER; }

    /** Latin letters used in a rune string, for answer checks: maps each rune to its first sound. */
    public static Rune byGlyph(Track t, char c) {
        for (Rune r : t.runes) if (r.glyph.charAt(0) == c) return r;
        return null;
    }
}
