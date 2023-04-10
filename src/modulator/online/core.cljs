(ns modulator.online.core
  (:require [modulator.number-utils :as nu]
            [modulator.core :as m]
            [clojure.string :as cstr]
            [clojure.set]))

(defn set-element-text
  "Nastaví text na element bez té super krkolomné syntaxe."
  [element text]
  (let [el (js/document.getElementById element)]
    (set! (.-textContent el) text)))

(defn get-element-property
  "Takhle to úplně nefunguje no"
  [element property]
  (let [el (js/document.getElementById element)]
    (.-value el)))

(defn vymaz-vystupy
  "Smaže všechny vystupy v případě erroru"
  [seznam]
  (when (not= 0 (count seznam))
    (set-element-text (first seznam) "")
    (recur (rest seznam))))

(defn bin-to-znak
  "Převede binární znak na jeho ASCII reprezentaci
   Přepsání originální java funkce protože se s portabilitou zas tak nepočítalo."
  [bin-znak]
  (str (js/parseInt bin-znak 2)))

(defn bin-to-string
  "Převede binární String do ASCII stringu"
  [bin-str]
  (apply str (map (fn
                    [lst]
                    (bin-to-znak (apply str lst)))
                  (partition 8 bin-str))))

(defn try-decode
  "Jenom vyzkouší dekodování a vrací hezky naformátovanou chybu když je nemožné provézt demodulaci danou funkcí"
  [decode-fun vstup]
  (try
    (bin-to-string (decode-fun vstup))
    (catch ExceptionInfo e
      (let [data (ex-data e)
            prebyva (:prebyvajici-data data)
            uspesne (:uspecna-demodulace data)]
        (str (ex-message e)
             (when uspesne
               (str " úspěšná část: \"" uspesne))
             (when prebyva
               (str "\" Přebývá vám: \"" prebyva "\"")))))))

(defn demoduluj-vstup
  "Provede všechny demodulace, ty které nelze provézt budou označeny"
  []
  ;; Todo: Při zpětném převodu z binárky přijímat znak jen pokud je v ASCII rozsahu
  ;;       Zachytí se tak více neplatných sekvencí
  (set-element-text "error-text" "")
  (set-element-text "modulace-info" "Demodulováno")
  (let [input (get-element-property "text-vstup" "value")
        fm (try-decode m/fm-decode input)
        mfm (try-decode m/mfm-decode input)
        rll1 (try-decode (fn [x] (m/rll-decode m/RLL-1 x)) input)
        rll2 (try-decode (fn [x] (m/rll-decode m/RLL-2 x)) input)]
    (set-element-text "FM-modul" fm)
    (set-element-text "MFM-modul" mfm)
    (set-element-text "RLL1-modul" rll1)
    (set-element-text "RLL2-modul" rll2))
  (vymaz-vystupy '("FM-pulz"
                   "MFM-pulz"
                   "RLL1-pulz"
                   "RLL2-pulz"
                   "bin-out")))

(defn zmoduluj-vstup
  "Provede všechny modulace na text na vstupu"
  []
  (set-element-text "error-text" "")
  (set-element-text "modulace-info" "Modulováno")
  (try
    (let [input (get-element-property "text-vstup" "value")
          text-bin (apply str (map nu/znak-to-bin input))
          text-fm (m/fm-encode text-bin)
          text-mfm (m/mfm-encode text-bin)
          text-rll1 (m/rll-encode m/RLL-1 text-bin)
          text-rll2 (m/rll-encode m/RLL-2 text-bin)]
      (set-element-text "bin-out" text-bin)

      (set-element-text "FM-modul" text-fm)
      (set-element-text "FM-pulz" (m/get-efektivita text-fm))

      (set-element-text "MFM-modul" text-mfm)
      (set-element-text "MFM-pulz" (m/get-efektivita text-mfm))

      (set-element-text "RLL1-modul" text-rll1)
      (set-element-text "RLL1-pulz" (m/get-efektivita text-rll1))

      (set-element-text "RLL2-modul" text-rll2)
      (set-element-text "RLL2-pulz" (m/get-efektivita text-rll2)))
    (catch ExceptionInfo e
      (vymaz-vystupy '("FM-modul" "FM-pulz"
                       "MFM-modul" "MFM-pulz"
                       "RLL1-modul" "RLL1-pulz"
                       "RLL2-modul" "RLL2-pulz"
                       "bin-out"))
      (set-element-text "error-text" (ex-message e)))))

(.addEventListener (js/document.getElementById "moduluj-cudl") "click" zmoduluj-vstup)
(.addEventListener (js/document.getElementById "demoduluj-cudl") "click" demoduluj-vstup)
