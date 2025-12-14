package com.example.bikeassist.blindview

import com.example.bikeassist.util.AppLogger

data class SpokenLabel(
    val en: String,
    val deSingular: String,
    val dePlural: String
)

interface LabelTranslator {
    fun translate(en: String): SpokenLabel
}

class CocoLabelTranslator : LabelTranslator {
    private val labels: Map<String, SpokenLabel> = mapOf(
        "person" to SpokenLabel("person", "Person", "Personen"),
        "bicycle" to SpokenLabel("bicycle", "Fahrrad", "Fahrraeder"),
        "car" to SpokenLabel("car", "Auto", "Autos"),
        "motorcycle" to SpokenLabel("motorcycle", "Motorrad", "Motorraeder"),
        "airplane" to SpokenLabel("airplane", "Flugzeug", "Flugzeuge"),
        "bus" to SpokenLabel("bus", "Bus", "Busse"),
        "train" to SpokenLabel("train", "Zug", "Zuege"),
        "truck" to SpokenLabel("truck", "Lkw", "Lkws"),
        "boat" to SpokenLabel("boat", "Boot", "Boote"),
        "traffic light" to SpokenLabel("traffic light", "Ampel", "Ampeln"),
        "fire hydrant" to SpokenLabel("fire hydrant", "Hydrant", "Hydranten"),
        "stop sign" to SpokenLabel("stop sign", "Stoppschild", "Stoppschilder"),
        "parking meter" to SpokenLabel("parking meter", "Parkuhr", "Parkuhren"),
        "bench" to SpokenLabel("bench", "Bank", "Baenke"),
        "bird" to SpokenLabel("bird", "Vogel", "Voegel"),
        "cat" to SpokenLabel("cat", "Katze", "Katzen"),
        "dog" to SpokenLabel("dog", "Hund", "Hunde"),
        "horse" to SpokenLabel("horse", "Pferd", "Pferde"),
        "sheep" to SpokenLabel("sheep", "Schaf", "Schaefe"),
        "cow" to SpokenLabel("cow", "Kuh", "Kuehe"),
        "elephant" to SpokenLabel("elephant", "Elefant", "Elefanten"),
        "bear" to SpokenLabel("bear", "Baer", "Baeren"),
        "zebra" to SpokenLabel("zebra", "Zebra", "Zebras"),
        "giraffe" to SpokenLabel("giraffe", "Giraffe", "Giraffen"),
        "backpack" to SpokenLabel("backpack", "Rucksack", "Ruecksaecke"),
        "umbrella" to SpokenLabel("umbrella", "Regenschirm", "Regenschirme"),
        "handbag" to SpokenLabel("handbag", "Handtasche", "Handtaschen"),
        "tie" to SpokenLabel("tie", "Krawatte", "Krawatten"),
        "suitcase" to SpokenLabel("suitcase", "Koffer", "Koffer"),
        "frisbee" to SpokenLabel("frisbee", "Frisbee", "Frisbees"),
        "skis" to SpokenLabel("skis", "Ski", "Ski"),
        "snowboard" to SpokenLabel("snowboard", "Snowboard", "Snowboards"),
        "sports ball" to SpokenLabel("sports ball", "Ball", "Baelle"),
        "kite" to SpokenLabel("kite", "Drachen", "Drachen"),
        "baseball bat" to SpokenLabel("baseball bat", "Baseballschlaeger", "Baseballschlaeger"),
        "baseball glove" to SpokenLabel("baseball glove", "Baseballhandschuh", "Baseballhandschuhe"),
        "skateboard" to SpokenLabel("skateboard", "Skateboard", "Skateboards"),
        "surfboard" to SpokenLabel("surfboard", "Surfbrett", "Surfbretter"),
        "tennis racket" to SpokenLabel("tennis racket", "Tennisschlaeger", "Tennisschlaeger"),
        "bottle" to SpokenLabel("bottle", "Flasche", "Flaschen"),
        "wine glass" to SpokenLabel("wine glass", "Weinglas", "Weinglaeser"),
        "cup" to SpokenLabel("cup", "Tasse", "Tassen"),
        "fork" to SpokenLabel("fork", "Gabel", "Gabeln"),
        "knife" to SpokenLabel("knife", "Messer", "Messer"),
        "spoon" to SpokenLabel("spoon", "Loeffel", "Loeffel"),
        "bowl" to SpokenLabel("bowl", "Schuessel", "Schuesseln"),
        "banana" to SpokenLabel("banana", "Banane", "Bananen"),
        "apple" to SpokenLabel("apple", "Apfel", "Aepfel"),
        "sandwich" to SpokenLabel("sandwich", "Sandwich", "Sandwiches"),
        "orange" to SpokenLabel("orange", "Orange", "Orangen"),
        "broccoli" to SpokenLabel("broccoli", "Brokkoli", "Brokkoli"),
        "carrot" to SpokenLabel("carrot", "Karotte", "Karotten"),
        "hot dog" to SpokenLabel("hot dog", "Hotdog", "Hotdogs"),
        "pizza" to SpokenLabel("pizza", "Pizza", "Pizzen"),
        "donut" to SpokenLabel("donut", "Donut", "Donuts"),
        "cake" to SpokenLabel("cake", "Kuchen", "Kuchen"),
        "chair" to SpokenLabel("chair", "Stuhl", "Stuehle"),
        "couch" to SpokenLabel("couch", "Sofa", "Sofas"),
        "potted plant" to SpokenLabel("potted plant", "Topfpflanze", "Topfpflanzen"),
        "bed" to SpokenLabel("bed", "Bett", "Betten"),
        "dining table" to SpokenLabel("dining table", "Esstisch", "Esstische"),
        "toilet" to SpokenLabel("toilet", "Toilette", "Toiletten"),
        "tv" to SpokenLabel("tv", "Fernseher", "Fernseher"),
        "laptop" to SpokenLabel("laptop", "Laptop", "Laptops"),
        "mouse" to SpokenLabel("mouse", "Maus", "Maeuse"),
        "remote" to SpokenLabel("remote", "Fernbedienung", "Fernbedienungen"),
        "keyboard" to SpokenLabel("keyboard", "Tastatur", "Tastaturen"),
        "cell phone" to SpokenLabel("cell phone", "Handy", "Handys"),
        "microwave" to SpokenLabel("microwave", "Mikrowelle", "Mikrowellen"),
        "oven" to SpokenLabel("oven", "Ofen", "Oefen"),
        "toaster" to SpokenLabel("toaster", "Toaster", "Toaster"),
        "sink" to SpokenLabel("sink", "Spuele", "Spuelen"),
        "refrigerator" to SpokenLabel("refrigerator", "Kuehlschrank", "Kuehlschraenke"),
        "book" to SpokenLabel("book", "Buch", "Buecher"),
        "clock" to SpokenLabel("clock", "Uhr", "Uhren"),
        "vase" to SpokenLabel("vase", "Vase", "Vasen"),
        "scissors" to SpokenLabel("scissors", "Schere", "Scheren"),
        "teddy bear" to SpokenLabel("teddy bear", "Teddybaer", "Teddybaeren"),
        "hair drier" to SpokenLabel("hair drier", "Foen", "Foene"),
        "toothbrush" to SpokenLabel("toothbrush", "Zahnbuerste", "Zahnbuersten")
    )

    override fun translate(en: String): SpokenLabel {
        val key = en.lowercase()
        return labels[key] ?: SpokenLabel(en, en, "${en}s")
    }

    fun validateAgainst(labelsToCheck: List<String>) {
        if (labelsToCheck.isEmpty()) return
        val lowerKeys = labels.keys
        val missing = labelsToCheck.filter { !lowerKeys.contains(it.lowercase()) }
        if (missing.isNotEmpty()) {
            AppLogger.e(message = "CocoLabelTranslator missing mappings for: $missing")
        }
    }
}
