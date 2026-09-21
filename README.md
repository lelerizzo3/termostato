# Termostato intelligente

Applicazione Java/Spring Boot 4 per il controllo di una caldaia tramite sensore di temperatura e relay REST esterni.

## Requisiti

- Java 21+
- Maven 3.9+
- Nessun server database e nessun container: i log sono salvati in SQLite su file.

## Build ed esecuzione

```powershell
mvn clean package
java -jar target/termostato.jar
```

Al primo avvio vengono creati automaticamente il database SQLite e i file JSON di configurazione/calendario nella directory `data` (se non diversamente configurato). I parametri di bootstrap possono essere sovrascritti, per esempio:

```powershell
java -jar target/termostato.jar `
  --termostato.database-path=C:/termostato/data/termostato.db `
  --termostato.sensore.url=http://192.168.1.20 `
  --termostato.relay.url=http://192.168.1.21
```

## API principali

- `GET /stato` — temperatura e umidità interne, target, stato relay, temperatura e umidità esterne
- `GET/PUT /config`
- `GET/PUT /config/calendario`
- `GET /log?da=YYYY-MM-DD&a=YYYY-MM-DD`
- `GET /log/errori?da=YYYY-MM-DD&a=YYYY-MM-DD`
- `GET /actuator/health`

### API dispositivi Shelly

- `GET /shelly/dispositivi` — lista di tutti i dispositivi Shelly configurati con metadati (id, nome, ip, tipo, modello, generazione)
- `GET /shelly/relay/{id}` — stato corrente del relay (acceso/spento) con metadati del dispositivo
- `POST /shelly/relay/{id}/on` — accende il relay; risponde con lo stato aggiornato
- `POST /shelly/relay/{id}/off` — spegne il relay; risponde con lo stato aggiornato
- `GET /shelly/consumi/{id}?da=YYYY-MM-DD&a=YYYY-MM-DD` — log consumi di un dispositivo PM per range temporale; risposta include metadati dispositivo e lista misure (potenza W, tensione V, corrente A)

## Meteo esterno e notifiche errori

Il servizio legge temperatura e umidità esterne tramite l'API REST pubblica [Open-Meteo](https://open-meteo.com/). Il default è il punto di riferimento di Acireale (Catania), ma URL e coordinate sono configurabili:

```yaml
termostato:
  meteo-esterno-url: https://api.open-meteo.com
  meteo-esterno-latitudine: 37.6167
  meteo-esterno-longitudine: 15.1667
  fuso-orario: Europe/Rome
  ora-legale: true
  notifiche-errori-abilitate: true
```

Gli intervalli del calendario sono interpretati nell'orario civile del `fuso-orario` configurato. Il default `Europe/Rome` applica automaticamente le regole italiane CET/CEST tramite il database IANA; `ora-legale: true` abilita questo comportamento. Se impostato a `false`, il calendario viene interpretato usando l'offset standard del fuso senza il passaggio estivo. I timestamp dei log restano memorizzati in UTC per mantenere un riferimento assoluto. `notifiche-errori-abilitate` controlla esclusivamente l'invio degli errori tramite ntfy; le notifiche informative di accensione/spegnimento restano controllate da `debug-mode`. Se Open-Meteo non è raggiungibile durante il polling, il controllo locale sensore/relay continua; il record di polling mantiene vuote le misure esterne e viene registrato un errore in `error_log` con categoria `READ_WEATHER`. Gli errori di lettura meteo esterno **non** generano notifiche ntfy indipendentemente da `notifiche-errori-abilitate`: si tratta di un servizio terzo informativo e la sua irraggiungibilità non deve produrre rumore sulle notifiche.


Tutti gli endpoint REST richiedono l'header `X-API-Key`. La lista è configurabile tramite `api_keys`/`api-keys`; una chiave assente o non valida restituisce HTTP 401. Il default è vuoto (fail-closed), quindi configurare almeno una chiave prima di usare le API:

```yaml
termostato:
  api-keys:
    - sostituire-con-una-chiave-segreta
```

Oppure al bootstrap:

```powershell
java -jar target/termostato.jar --termostato.api-keys[0]=sostituire-con-una-chiave-segreta
```

Esempio di chiamata:

```powershell
curl.exe -H "X-API-Key: sostituire-con-una-chiave-segreta" http://localhost:8080/config
```

Le modifiche via `PUT` sono applicate immediatamente e persistite nei file JSON configurati. La configurazione persistita prevale sui default YAML ai riavvii. Il `database_path` è un parametro di bootstrap: si configura all'avvio e non può essere cambiato a runtime, perché determina il datasource SQLite già aperto.

Il contratto tecnico completo è in [`docs/specifiche-tecniche.md`](docs/specifiche-tecniche.md); i requisiti funzionali sono in [`docs/specifiche-funzionali.md`](docs/specifiche-funzionali.md). La specifica OpenAPI/Swagger per il frontend è in [`docs/openapi.yaml`](docs/openapi.yaml).

## Dispositivi Shelly

I dispositivi Shelly sono configurati in un file JSON separato (default `data/shelly.json`). Al primo avvio il file viene creato automaticamente con la lista dei dispositivi preconfigurati. Il file è read-only a runtime.

Parametri di bootstrap relativi ai dispositivi Shelly:

```yaml
termostato:
  shelly-file: ./data/shelly.json
  intervallo-polling-consumi-secondi: 30   # frequenza polling dei PM
  retention-consumi-giorni: 90             # retention log consumi (indipendente da retention-log-giorni)
```

Il polling dei power monitor (dispositivi di tipo PM) avviene in modo separato rispetto al ciclo di controllo del termostato, con frequenza configurabile tramite `intervallo-polling-consumi-secondi`. Gli errori di lettura di singoli dispositivi vengono loggati in `error_log` con categoria `SHELLY_PM` e non bloccano il polling degli altri device.


## Profilo mock e test E2E

Per eseguire il test mock è necessario attivare il profilo Spring `mock`: `debug-mode` controlla esclusivamente le notifiche informative ntfy e non registra gli endpoint mock.

```powershell
mvn clean package
java -jar target/termostato.jar --spring.profiles.active=mock --termostato.debug-mode=true
```

Lo script E2E equivalente è:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\e2e-mock.ps1
```

Gli endpoint disponibili sono:
- `GET /relay` — stato del relay simulato;
- `POST /relay` — comando del relay simulato;
- `PUT /mock/temperature` — modifica temperatura e umidità simulate;
- `GET /mock/state` — stato utile allo scenario E2E;
- `POST /mock/reset` — ripristino dello stato iniziale.

Il controllo usa comunque i client REST normali, puntati a `http://localhost:8080`. Il profilo mock configura la chiave `e2e-test-key`; lo script la invia nell'header `X-API-Key` e verifica che una chiave errata produca HTTP 401. La configurazione persistita in `data/mock-config.json` prevale sui default di `application-mock.yml`: se il file esiste già e contiene `api_keys` vuote o diverse, `e2e-test-key` non viene aggiunta automaticamente. Per ripristinare la configurazione mock iniziale, arrestare l'applicazione e spostare il file (oppure modificare manualmente `api_keys`):

```powershell
if (Test-Path .\data\mock-config.json) {
  Move-Item .\data\mock-config.json .\data\mock-config.json.bak -Force
}
```

Al riavvio il file viene ricreato con `e2e-test-key`. Lo stato del relay mantenuto in RAM appartiene esclusivamente al dispositivo mock; il controllo continua a leggere lo stato dal client relay e non memorizza lo stato della caldaia.

Il client ntfy non viene simulato: il profilo mantiene `https://ntfy.sh`. Lo script lascia `debug_mode=false` per non inviare notifiche informative durante il test; usare `-DebugNtfy` per abilitare l'invio reale delle notifiche di accensione/spegnimento.

Il profilo mock non ha alcun effetto sugli endpoint `/shelly/*`: i dispositivi Shelly vengono sempre interrogati ai loro IP reali nella rete locale, sia con che senza profilo mock.
