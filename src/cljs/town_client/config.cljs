(ns town-client.config)

(def url-base "http://dev.hel.fi/kenenkaupunki/api")

(def aggregates
  {:asuminen #{"paikka-tai-alue-asuinrakentamiselle"
               "alue-ei-ole-virkistykselle-valttamaton-paikalle-voisi-rakentaa"}
  :palvelut #{"paikka-toimistoille-palveluille-tai-liiketiloille"
              "taalla-pitaisi-olla-enemman-kauppoja-ja-palveluita-rakennusten-kivijaloissa"}
  :virkistys #{"virkistyksellisesti-tarkea-mutta-saisi-olla-laadultaan-parempi"
               "taalla-on-tallaisenaan-ainutlaatuista-kaupunkiluontoa"}
  :ankeus #{"taalla-ymparisto-nayttaa-ankealta-ja-sita-pitaisi-parantaa-esimerkiksi-puuistutuksin"
            "huonosti-hoidettu-epamaarainen-alue-jota-tulisi-parantaa"}})

(def front-page-map-styles
  #js[
      #js{
       "stylers"
       #js[#js{"visibility" "off"}
           #js{ "hue" "#00ffe6"}
           #js{ "saturation" -20}]}
      #js{"featureType" "road"
       "elementType" "geometry"
       "stylers" #js[
                 #js{ "lightness" 100 }
                 #js{ "visibility" "simplified" }
                 ]
       }
      #js{
       "featureType" "road",
       "elementType" "labels"
       "stylers" #js[
                 #js{ "visibility" "off" }
                 ]
       }
      #js{
          "featureType" "water"
          "stylers" #js[
                      #js{"visibility" "simple"}
                      #js{"color" "#000000"}
                      ]
          }

      ]
)
