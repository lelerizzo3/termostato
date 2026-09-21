# Evolutiva backend: integrazione dispositivi Shelly

Documento di handoff per l'agente frontend. Descrive tutte le modifiche apportate al backend nella evolutiva "integrazione dispositivi Shelly": nuove API, struttura delle risposte, codici di errore e parametri di configurazione.

---

## Profilo mock e dispositivi Shelly

Il profilo Spring `mock` simula **esclusivamente** il sensore di temperatura/umidità e il relay del termostato (che puntano a `localhost:8080` invece che ai device fisici). **Tutti gli endpoint `/shelly/*` funzionano allo stesso modo con o senza il profilo mock**: i dispositivi Shelly vengono sempre interrogati ai loro IP reali nella rete locale.

In altre parole:
- Con profilo `mock`: termostato usa dati simulati, ma `GET /shelly/relay/portone` parla con `192.168.1.2` per davvero.
- Senza profilo `mock` (produzione): comportamento identico per tutto.

---

## Panoramica

Sono stati aggiunti sei nuovi endpoint REST sotto il path `/shelly`, tutti protetti dall'header `X-API-Key` già usato dagli endpoint esistenti:

| Metodo | Path | Descrizione |
|--------|------|-------------|
| `GET` | `/shelly/dispositivi` | Lista di tutti i dispositivi configurati con metadati |
| `GET` | `/shelly/relay/{id}` | Stato corrente di un relay |
| `POST` | `/shelly/relay/{id}/on` | Accende un relay |
| `POST` | `/shelly/relay/{id}/off` | Spegne un relay |
| `GET` | `/shelly/consumi/{id}` | Log consumi di un power monitor (con range temporale) |

---

## Dispositivi configurati

I device sono definiti nel file `data/shelly.json` (creato automaticamente all'avvio). L'elenco attuale:

| id | nome | ip | tipo | modello | generazione |
|----|------|----|------|---------|-------------|
| `portone` | Portone | 192.168.1.2 | `RELAY` | SHSW-1 | 1 |
| `cancello` | Cancello | 192.168.1.3 | `RELAY` | S3SW-001X8EU | 3 |
| `terrazzo` | Terrazzo | 192.168.1.10 | `RELAY` | S3SW-001X8EU | 3 |
| `frigo-e-forno` | Frigo e Forno | 192.168.1.9 | `PM` | SNPM-001PCEU16 | 3 |
| `top-cucina` | Top Cucina | 192.168.1.7 | `PM` | SNPM-001PCEU16 | 3 |
| `lavanderia` | Lavanderia | 192.168.1.12 | `PM` | SNPM-001PCEU16 | 3 |
| `modem` | Modem | 192.168.1.5 | `PM` | SNPM-001PCEU16 | 3 |
| `tv-cucina` | Tv Cucina | 192.168.1.8 | `PM` | SNPM-001PCEU16 | 3 |
| `lavastoviglie-e-cappa` | Lavastoviglie e Cappa | 192.168.1.14 | `PM` | SNPM-001PCEU16 | 3 |

Tipi possibili:
- `RELAY` — dispositivo con relay comandabile (accendi/spegni)
- `PM` — power monitor (solo lettura consumi, non ha relay comandabile)

---

## API dettaglio

### `GET /shelly/dispositivi`

Restituisce la lista completa di tutti i dispositivi con i loro metadati. Utile per costruire dinamicamente la UI senza hardcodare gli id.

**Response 200:**
```json
[
  {
    "id": "portone",
    "nome": "Portone",
    "ip": "192.168.1.2",
    "tipo": "RELAY",
    "modello": "SHSW-1",
    "generazione": 1
  },
  {
    "id": "frigo-e-forno",
    "nome": "Frigo e Forno",
    "ip": "192.168.1.9",
    "tipo": "PM",
    "modello": "SNPM-001PCEU16",
    "generazione": 3
  }
]
```

---

### `GET /shelly/relay/{id}`

Legge lo stato corrente del relay (chiamata live al dispositivo).

**Path parameter:** `id` — id del dispositivo (es. `portone`, `terrazzo`)

**Response 200:**
```json
{
  "id": "portone",
  "nome": "Portone",
  "ip": "192.168.1.2",
  "modello": "SHSW-1",
  "generazione": 1,
  "tipo": "RELAY",
  "acceso": false
}
```

**Errori:**
- `400` — il dispositivo esiste ma è di tipo `PM` (non ha relay comandabile)
- `404` — id non trovato

---

### `POST /shelly/relay/{id}/on`

Accende il relay, poi rilegge lo stato dal dispositivo e lo restituisce.

**Response 200:** stesso schema di `GET /shelly/relay/{id}`, con `"acceso": true` atteso.

> Nota: il Portone e il Cancello sono configurati con autospegnimento hardware dopo 1 secondo. Il backend non gestisce questa logica: se il frontend rilegge lo stato dopo l'autospegnimento, `acceso` sarà `false`.

---

### `POST /shelly/relay/{id}/off`

Spegne il relay, poi rilegge lo stato.

**Response 200:** stesso schema di `GET /shelly/relay/{id}`, con `"acceso": false` atteso.

---

### `GET /shelly/consumi/{id}?da=YYYY-MM-DD&a=YYYY-MM-DD`

Restituisce i log dei consumi di un dispositivo PM per range temporale. Stessa semantica dei parametri `da`/`a` del `GET /log`: le date sono inclusive, interpretate in UTC (`[da 00:00:00Z, a+1 00:00:00Z)`). Senza parametri usa il giorno corrente UTC.

**Path parameter:** `id` — id del dispositivo (es. `frigo-e-forno`)

**Query parameters:**
- `da` (opzionale) — data inizio in formato `YYYY-MM-DD`
- `a` (opzionale) — data fine in formato `YYYY-MM-DD`

**Response 200:**
```json
{
  "id": "frigo-e-forno",
  "nome": "Frigo e Forno",
  "ip": "192.168.1.9",
  "modello": "SNPM-001PCEU16",
  "generazione": 3,
  "tipo": "PM",
  "misure": [
    {
      "id": 1,
      "data_ora": "2026-09-20T17:00:00Z",
      "device_id": "frigo-e-forno",
      "potenza_w": 45.2,
      "tensione_v": 229.8,
      "corrente_a": 0.197
    }
  ]
}
```

Campi di ogni misura:
- `id` — chiave primaria SQLite
- `data_ora` — timestamp UTC del polling
- `device_id` — id del dispositivo (ridondante, comodo per serializzazione)
- `potenza_w` — potenza istantanea in Watt (nullable se la lettura non ha restituito il valore)
- `tensione_v` — tensione in Volt (nullable)
- `corrente_a` — corrente in Ampere (nullable)

**Errori:**
- `400` — `da` successivo ad `a`, oppure il dispositivo esiste ma è di tipo `RELAY` (non ha consumi)
- `404` — id non trovato

---

## Codici di errore aggiornati

### Nuovo: `404 Not Found` per device inesistente

Tutti gli endpoint `/shelly/*` che ricevono un `{id}` non riconosciuto restituiscono ora `404` con il body `ApiError` standard (già usato per gli errori 400 e 500):

```json
{
  "timestamp": "2026-09-20T17:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Dispositivo Shelly non trovato: inesistente"
}
```

> Questo handler `NoSuchElementException → 404` è globale: vale anche per eventuali futuri endpoint che lanciano la stessa eccezione.

---

## Struttura JSON — naming convention

Come per tutti gli endpoint esistenti, il backend usa la strategia Jackson `SNAKE_CASE`:
- `device_id`, `potenza_w`, `tensione_v`, `corrente_a`, `data_ora`

I campi dell'enum `ShellyTipo` sono serializzati come stringa maiuscola: `"RELAY"` o `"PM"`.

---

## Polling dei consumi

I dati di `GET /shelly/consumi/{id}` vengono popolati dal backend tramite uno scheduler interno che interroga i device PM ogni `intervallo-polling-consumi-secondi` (default: 30 secondi). I log vengono conservati per `retention-consumi-giorni` (default: 90 giorni) e poi eliminati automaticamente ogni ora.

Gli errori di lettura di singoli device non interrompono il polling degli altri e vengono registrati nella tabella `error_log` con categoria `SHELLY_PM` (visibili tramite `GET /log/errori`). Gli errori `SHELLY_PM` **non generano notifiche ntfy**, esattamente come gli errori `READ_WEATHER` del meteo esterno: si tratta di servizi terzi informativi la cui irraggiungibilità non deve produrre rumore sulle notifiche.

---

## Nuovi parametri di configurazione

Questi parametri sono di bootstrap (definiti all'avvio) e non modificabili a runtime tramite `PUT /config`:

| Parametro YAML | Default | Descrizione |
|----------------|---------|-------------|
| `termostato.shelly-file` | `./data/shelly.json` | Path del file JSON dei device |
| `termostato.intervallo-polling-consumi-secondi` | `30` | Frequenza polling PM in secondi |
| `termostato.retention-consumi-giorni` | `90` | Giorni di retention dei log consumi |

---

## Autenticazione

Invariata: tutti gli endpoint `/shelly/*` richiedono l'header `X-API-Key` con una chiave presente in `api_keys`. Una chiave assente o non valida restituisce `401`.

---

## Riferimento completo

La specifica OpenAPI aggiornata è in `docs/openapi.yaml`.
