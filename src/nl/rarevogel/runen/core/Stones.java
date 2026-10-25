package nl.rarevogel.runen.core;

import java.util.ArrayList;
import java.util.List;

import nl.rarevogel.runen.core.Runes.Track;

/**
 * Real inscriptions to read word by word. Transliterations follow the Scandinavian Runic-text
 * Database (Rundata) and the usual readings of the English objects; R is the final r-rune.
 */
public final class Stones {

    public static final class Word {
        public final String runes, translit, norm, gloss, pron;
        Word(String runes, String translit, String norm, String gloss, String pron) {
            this.runes = runes; this.translit = translit; this.norm = norm; this.gloss = gloss; this.pron = pron;
        }
    }

    public static final class Stone {
        public final String id, title, place, date, intro, translation;
        public final Track track;
        public final List<Word> words = new ArrayList<>();
        public final boolean audio;

        Stone(String id, Track track, String title, String place, String date, String intro, String translation, boolean audio) {
            this.id = id; this.track = track; this.title = title; this.place = place; this.date = date;
            this.intro = intro; this.translation = translation; this.audio = audio;
        }

        Stone w(String translit, String norm, String gloss, String pron) {
            words.add(new Word(carve(track, translit), translit, norm, gloss, pron));
            return this;
        }

        Stone row(String runes, String translit, String gloss) {
            words.add(new Word(runes, translit, "", gloss, ""));
            return this;
        }

        /** Phoneme string of the whole text, for the audio generator. */
        public String pron() {
            StringBuilder b = new StringBuilder();
            for (Word w : words) if (w.pron.length() > 0) { if (b.length() > 0) b.append(' '); b.append(w.pron); }
            return b.toString();
        }
    }

    static final String ELDER_FROM = "fuþarkgwhnijpïzstbemlŋdoR", ELDER_TO = "ᚠᚢᚦᚨᚱᚲᚷᚹᚺᚾᛁᛃᛈᛇᛉᛊᛏᛒᛖᛗᛚᛜᛞᛟᛉ";
    static final String YOUNGER_FROM = "fuþąorkhniastbmlR", YOUNGER_TO = "ᚠᚢᚦᚬᚬᚱᚴᚼᚾᛁᛅᛋᛏᛒᛘᛚᛦ";
    static final String FUTHORC_FROM = "fuþorcgwhnijïpxstbeŋdlmœaæy", FUTHORC_TO = "ᚠᚢᚦᚩᚱᚳᚷᚹᚻᚾᛁᛄᛇᛈᛉᛋᛏᛒᛖᛝᛞᛚᛗᛟᚪᚫᚣ";

    /** Scholarly transliteration back to runes, letter by letter. */
    static String carve(Track t, String translit) {
        String from = t == Runes.ELDER ? ELDER_FROM : t == Runes.YOUNGER ? YOUNGER_FROM : FUTHORC_FROM;
        String to = t == Runes.ELDER ? ELDER_TO : t == Runes.YOUNGER ? YOUNGER_TO : FUTHORC_TO;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < translit.length(); i++) {
            char c = translit.charAt(i);
            if (t == Runes.FUTHORC && c == 'e' && i + 1 < translit.length() && translit.charAt(i + 1) == 'a') { b.append('ᛠ'); i++; continue; }
            int k = from.indexOf(c);
            if (k < 0) throw new IllegalArgumentException("no rune for " + c + " in " + translit);
            b.append(to.charAt(k));
        }
        return b.toString();
    }

    public static final List<Stone> ALL = new ArrayList<>();

    static {
        ALL.add(new Stone("kylver", Runes.ELDER, "De steen van Kylver", "Gotland, Zweden", "ca. 400",
                "De oudste bekende complete rij van de oude futhark, gekrast in een steen uit een graf. Waarom de runenrij in een graf lag, weet niemand. Aan het eind staat nog een teken dat op een dennenboompje lijkt.",
                "De hele runenrij: f u þ a r k g w · h n i j p ï z s · t b e m l ŋ d o", false)
                .row("ᚠᚢᚦᚨᚱᚲᚷᚹ", "fuþarkgw", "de eerste ætt")
                .row("ᚺᚾᛁᛃᛈᛇᛉᛊ", "hnijpïzs", "de tweede ætt; hier staat ᛈ vóór ᛇ")
                .row("ᛏᛒᛖᛗᛚᛜᛞᛟ", "tbemlŋdo", "de derde ætt"));

        ALL.add(new Stone("gallehus", Runes.ELDER, "De gouden hoorn van Gallehus", "Zuid-Jutland, Denemarken", "ca. 400",
                "Twee gouden drinkhoorns werden in 1639 en 1734 gevonden. In 1802 werden ze gestolen en omgesmolten; we kennen ze alleen nog van tekeningen. Op de tweede hoorn stond deze zin, in het Oernoors.",
                "Ik, Hlewagastir van Holt, maakte de hoorn.", true)
                .w("ek", "ek", "ik", "'ek")
                .w("hlewagastiR", "Hlewagastiz", "Hlewagastir, een naam: 'roemrijke gast'", "'hlewagastiz")
                .w("holtijaR", "Holtijaz", "van Holt, of: zoon van Holt", "'holtijaz")
                .w("horna", "horna", "de hoorn", "'horna")
                .w("tawido", "tawidō", "maakte", "'tawido:"));

        ALL.add(new Stone("jelling1", Runes.YOUNGER, "De kleine steen van Jelling", "Jelling, Denemarken", "ca. 950",
                "Koning Gorm de Oude liet deze steen maken voor zijn vrouw Thyra. Het is een van de oudste stenen waarop de naam Denemarken voorkomt.",
                "Koning Gorm maakte deze gedenktekens ter nagedachtenis aan Thyra, zijn vrouw, het sieraad van Denemarken.", true)
                .w("kurmR", "Gormr", "Gorm", "'gormr")
                .w("kunukR", "konungr", "koning", "'konuNgr")
                .w("karþi", "gerði", "maakte", "'gerDi")
                .w("kubl", "kuml", "gedenktekens", "'kuml")
                .w("þusi", "þessi", "deze", "'Tessi")
                .w("aft", "ept", "ter nagedachtenis aan", "'ept")
                .w("þurui", "Þyrvé", "Thyra", "'Tyrve:")
                .w("kunu", "konu", "vrouw", "'konu")
                .w("sina", "sína", "zijn", "'si:na")
                .w("tanmarkaR", "Danmarkar", "van Denemarken", "'danmarkar")
                .w("but", "bót", "sieraad (of: redding)", "'bo:t"));

        ALL.add(new Stone("jelling2", Runes.YOUNGER, "De grote steen van Jelling", "Jelling, Denemarken", "ca. 965",
                "Harald Blauwtand, de zoon van Gorm en Thyra, liet deze steen maken. Hij wordt wel de doopakte van Denemarken genoemd en staat op de Werelderfgoedlijst.",
                "Koning Harald liet deze gedenktekens maken ter nagedachtenis aan Gorm, zijn vader, en aan Thyra, zijn moeder. Dat is de Harald die heel Denemarken en Noorwegen voor zich won en de Denen christen maakte.", true)
                .w("haraltr", "Haraldr", "Harald", "'haraldr")
                .w("kunukR", "konungr", "koning", "'konuNgr")
                .w("baþ", "bað", "liet", "'baD")
                .w("kaurua", "gera", "maken", "'gerva")
                .w("kubl", "kuml", "gedenktekens", "'kuml")
                .w("þausi", "þessi", "deze", "'Tessi")
                .w("aft", "ept", "ter nagedachtenis aan", "'ept")
                .w("kurm", "Gorm", "Gorm", "'gorm")
                .w("faþur", "fǫður", "vader", "'fODur")
                .w("sin", "sinn", "zijn", "'sinn")
                .w("auk", "ok", "en", "'ok")
                .w("aft", "ept", "ter nagedachtenis aan", "'ept")
                .w("þąurui", "Þyrvé", "Thyra", "'Tyrve:")
                .w("muþur", "móður", "moeder", "'mo:Dur")
                .w("sina", "sína", "zijn", "'si:na")
                .w("sa", "sá", "die", "'sa:")
                .w("haraltr", "Haraldr", "Harald", "'haraldr")
                .w("ias", "er", "die", "'er")
                .w("sąR", "sér", "voor zichzelf", "'se:r")
                .w("uan", "vann", "won", "'vann")
                .w("tanmaurk", "Danmǫrk", "Denemarken", "'danmOrk")
                .w("ala", "alla", "helemaal", "'alla")
                .w("auk", "ok", "en", "'ok")
                .w("nuruiak", "Norveg", "Noorwegen", "'norveg")
                .w("auk", "ok", "en", "'ok")
                .w("tani", "Dani", "de Denen", "'dani")
                .w("karþi", "gerði", "maakte", "'gerDi")
                .w("kristną", "kristna", "christen", "'kristna"));

        ALL.add(new Stone("gripsholm", Runes.YOUNGER, "De Ingvarsteen van Gripsholm", "Södermanland, Zweden", "ca. 1040",
                "Tola liet deze steen maken voor haar zoon Harald, die met Ingvar de Verrereiziger naar het oosten trok. Bijna niemand kwam van die tocht terug. Het tweede deel van de tekst is een vers. Serkland was het land van de Saracenen, ergens rond de Kaspische Zee.",
                "Tola liet deze steen oprichten ter nagedachtenis aan haar zoon Harald, de broer van Ingvar. Zij trokken dapper ver weg om goud, en in het oosten gaven ze de adelaar te eten. Ze stierven in het zuiden, in Serkland.", true)
                .w("tula", "Tóla", "Tola", "'to:la")
                .w("lit", "lét", "liet", "'le:t")
                .w("raisa", "reisa", "oprichten", "'reisa")
                .w("stain", "stein", "steen", "'stein")
                .w("þinsa", "þenna", "deze", "'Tenna")
                .w("at", "at", "ter nagedachtenis aan", "'at")
                .w("sun", "son", "zoon", "'son")
                .w("sin", "sinn", "haar", "'sinn")
                .w("haralt", "Harald", "Harald", "'harald")
                .w("bruþur", "bróður", "broer", "'bro:Dur")
                .w("inkuars", "Ingvars", "van Ingvar", "'iNgvars")
                .w("þaiR", "þeir", "zij", "'Teir")
                .w("furu", "fóru", "trokken", "'fo:ru")
                .w("trikila", "drengila", "dapper", "'dreNgila")
                .w("fiari", "fjarri", "ver weg", "'fjarri")
                .w("at", "at", "om", "'at")
                .w("kuli", "gulli", "goud", "'gulli")
                .w("auk", "ok", "en", "'ok")
                .w("austarla", "austarla", "in het oosten", "'austarla")
                .w("arni", "erni", "de adelaar", "'erni")
                .w("kafu", "gáfu", "gaven (te eten)", "'ga:fu")
                .w("tuu", "dóu", "stierven", "'do:u")
                .w("sunarla", "sunnarla", "in het zuiden", "'sunnarla")
                .w("a", "á", "in", "'a:")
                .w("sirklanti", "Serklandi", "Serkland", "'serklandi"));

        ALL.add(new Stone("beagnoth", Runes.FUTHORC, "Het mes van Beagnoth", "uit de Theems bij Londen", "10e eeuw",
                "Een lang mes (een sax), in 1857 in de Theems gevonden. Op het lemmet staat de complete Angelsaksische runenrij, ingelegd met koper, messing en zilverdraad, en de naam Beagnoth. Een paar runen staan in een ongewone volgorde.",
                "De runenrij, gevolgd door de naam Beagnoth, de eigenaar of de smid.", true)
                .row("ᚠᚢᚦᚩᚱᚳᚷᚹ", "fuþorcgw", "de eerste ætt")
                .row("ᚻᚾᛁᛄᛇᛈᛉᛋ", "hnijïpxs", "de tweede ætt")
                .row("ᛏᛒᛖᛝᛞᛚᛗᛟ", "tbeŋdlmœ", "de derde ætt; ᛝᛞᛚᛗ staan door elkaar")
                .row("ᚪᚫᚣᛠ", "aæyea", "de vier extra runen")
                .w("beagnoþ", "Beagnoþ", "Beagnoth, een Angelsaksische naam", "'bEaQnoT"));

        ALL.add(new Stone("franks", Runes.FUTHORC, "Het walvisbenen kistje", "Northumbria, Engeland; nu in het British Museum", "begin 8e eeuw",
                "Het Franks Casket is een kistje van walvisbeen, rondom versierd met scènes en runen. Op de voorkant staat een raadsel in versvorm: waar is dit kistje van gemaakt? De betekenis van 'gasric' is niet zeker.",
                "De vloed wierp de vis op de bergachtige kust; de koning van de verschrikking werd bedroefd toen hij op het grind zwom. Walvisbeen.", true)
                .w("fisc", "fisc", "vis", "'fiS")
                .w("flodu", "flōdu", "de vloed", "'flo:du")
                .w("ahof", "āhōf", "wierp op", "a'ho:f")
                .w("on", "on", "op", "'on")
                .w("fergenberig", "fergenberig", "de bergachtige kust", "'ferjenberij")
                .w("warþ", "warþ", "werd", "'warT")
                .w("gasric", "gāsric", "de koning van de verschrikking (de walvis?)", "'ga:sritS")
                .w("grorn", "grorn", "bedroefd", "'grorn")
                .w("þær", "þǣr", "toen", "'TE:r")
                .w("he", "hē", "hij", "'he:")
                .w("on", "on", "op", "'on")
                .w("greut", "grēut", "het grind", "'gre:ut")
                .w("giswom", "giswom", "zwom", "ji'swom")
                .w("hronæs", "hrōnæs", "van de walvis", "'hro:nEs")
                .w("ban", "bān", "been", "'ba:n"));
    }

    public static List<Stone> of(Track t) {
        List<Stone> r = new ArrayList<>();
        for (Stone s : ALL) if (s.track == t) r.add(s);
        return r;
    }
}
