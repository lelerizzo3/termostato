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

- `GET/PUT /config`
- `GET/PUT /config/calendario`
- `GET /log?da=YYYY-MM-DD&a=YYYY-MM-DD`
- `GET /log/errori?da=YYYY-MM-DD&a=YYYY-MM-DD`
- `GET /actuator/health`

Le modifiche via `PUT` sono applicate immediatamente e persistite nei file JSON configurati. La configurazione persistita prevale sui default YAML ai riavvii. Il `database_path` è un parametro di bootstrap: si configura all'avvio e non può essere cambiato a runtime, perché determina il datasource SQLite già aperto.

Il contratto tecnico completo è in [`docs/specifiche-tecniche.md`](docs/specifiche-tecniche.md); i requisiti funzionali sono in [`docs/specifiche-funzionali.md`](docs/specifiche-funzionali.md).


## Profilo mock e test E2E

Per eseguire un primo test senza sensore o relay fisici:

```powershell
mvn clean package
powershell -ExecutionPolicy Bypass -File .\scripts\e2e-mock.ps1
```

Lo script avvia il jar con il profilo Spring `mock`, che espone nello stesso processo:

- `GET /temperature` — risposta del sensore simulato;
- `GET /relay` — stato del relay simulato;
- `POST /relay` — comando del relay simulato;
- `PUT /mock/temperature` — modifica della temperatura simulata;
- `GET /mock/state` — stato utile allo scenario E2E;
- `POST /mock/reset` — ripristino dello stato iniziale.

Il controllo usa comunque i client REST normali, puntati a `http://localhost:8080`. Lo stato del relay mantenuto in RAM appartiene esclusivamente al dispositivo mock; il controllo continua a leggere lo stato dal client relay e non memorizza lo stato della caldaia.

Il client ntfy non viene simulato: il profilo mantiene `https://ntfy.sh`. Lo script lascia `debug_mode=false` per non inviare notifiche informative durante il test; usare `-DebugNtfy` per abilitare l'invio reale delle notifiche di accensione/spegnimento.
