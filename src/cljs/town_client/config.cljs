(ns town-client.config)

(def url-base "http://dev.hel.fi/kenenkaupunki/api/")
;(def url-base "http://localhost:8079/")

(def aggregates
  {:asuminen #{"paikka-tai-alue-asuinrakentamiselle"
               "alue-ei-ole-virkistykselle-valttamaton-paikalle-voisi-rakentaa"}
  :palvelut #{"paikka-toimistoille-palveluille-tai-liiketiloille"
              "taalla-pitaisi-olla-enemman-kauppoja-ja-palveluita-rakennusten-kivijaloissa"}
  :virkistys #{"virkistyksellisesti-tarkea-mutta-saisi-olla-laadultaan-parempi"
               "taalla-on-tallaisenaan-ainutlaatuista-kaupunkiluontoa"}
  :ankeus #{"taalla-ymparisto-nayttaa-ankealta-ja-sita-pitaisi-parantaa-esimerkiksi-puuistutuksin"
            "huonosti-hoidettu-epamaarainen-alue-jota-tulisi-parantaa"}})
