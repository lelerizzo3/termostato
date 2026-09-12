# Prompt: integrazione front-end della "fine forzatura" (`override_fine`)

## Ruolo e obiettivo

Sei uno sviluppatore front-end. Devi adeguare l'interfaccia del termostato intelligente
per supportare un nuovo campo di configurazione, `override_fine`, che permette di far
terminare automaticamente la forzatura manuale della temperatura a un istante prestabilito.
Il backend è già stato modificato e testato; il tuo compito riguarda **solo il front-end**.

Non modificare il backend. Basa l'integrazione sul contratto descritto qui sotto e sulla
specifica OpenAPI aggiornata in `docs/openapi.yaml`.

## Contesto funzionale

Il termostato ha una "forzatura manuale" (override): quando `override_attivo = true`, il
calendario settimanale viene ignorato e viene mantenuta `temperatura_override`.

La novità: la forzatura può avere una **fine opzionale**. Il campo `override_fine` indica
data e ora in cui la forzatura deve terminare; alla scadenza il sistema ripristina
automaticamente la normale operatività basata sul calendario.

## Cosa è cambiato nel backend

Nel modello di configurazione (`GET/PUT /config`, schema `SystemConfiguration`) è stato
aggiunto un campo:

| Campo | Tipo | Obbligatorio | Descrizione |
|---|---|---|---|
| `override_fine` | stringa `date-time` ISO **senza offset** (es. `2026-09-13T08:00:00`), oppure `null` | No | Istante di fine forzatura in **orario civile locale** |

Regole di comportamento del backend (da rispettare lato UI):

1. **Orario locale, non UTC.** `override_fine` è espresso nell'orario civile locale, con lo
   stesso fuso e le stesse regole di ora legale del calendario (`fuso_orario` / `ora_legale`
   presenti nella stessa configurazione). Il formato è ISO **senza** offset né suffisso `Z`
   (es. `2026-09-13T08:00:00`), esattamente come gli orari del calendario (`ora_inizio`/`ora_fine`)
   sono orari locali. NON inviare un istante UTC con `Z`.

2. **Opzionale e legato all'override.** `override_fine` è rilevante solo quando
   `override_attivo = true`. Se la forzatura non è attiva, il valore viene ignorato/azzerato
   dal backend. Se `override_attivo = true` e `override_fine` è assente/`null`, la forzatura
   resta **a tempo indeterminato** (comportamento storico invariato).

3. **Deve essere futuro (validazione 400).** Su `PUT /config`, se `override_attivo = true` e
   `override_fine` è valorizzato, deve essere un istante **futuro** rispetto all'ora corrente
   (confronto fatto in orario locale). Un valore già trascorso viene rifiutato con
   **HTTP 400** e corpo `ApiError` (vedi sotto). La UI deve gestire questo errore.

4. **Auto-disattivazione persistita.** Quando l'ora corrente raggiunge o supera
   `override_fine`, al ciclo di controllo successivo il backend:
   - imposta `override_attivo = false`,
   - azzera `temperatura_override` e `override_fine`,
   - **persiste** la configurazione (una sola volta),
   - invia una notifica informativa.
   Di conseguenza una successiva `GET /config` mostrerà `override_attivo = false`: la UI deve
   riflettere questo ripristino (idealmente con polling/refresh periodico dello stato di config,
   o al più tardi al successivo caricamento della pagina). Non serve alcuna azione del client
   per far scadere la forzatura: è il backend a gestirla.

## Contratto API di riferimento

- `GET /config` → restituisce `SystemConfiguration` completa, incluso `override_fine` (può
  essere `null`).
- `PUT /config` → riceve la `SystemConfiguration` completa. Risposte:
  - `200`: configurazione aggiornata e persistita (ritorna la config effettiva).
  - `400`: payload non valido, **incluso** `override_fine` non futuro con override attivo.
  - `401`: `X-API-Key` mancante o non valida.
  - `500`: errore di persistenza/interno.
- Tutte le chiamate richiedono l'header `X-API-Key`.

Esempio di `PUT /config` con forzatura a termine:

```json
{
  "soglia_attivazione": 0.3,
  "override_attivo": true,
  "temperatura_override": 20.5,
  "override_fine": "2026-09-13T08:00:00",
  "intervallo_polling_secondi": 60,
  "max_errori_consecutivi": 3,
  "retention_log_giorni": 30,
  "ntfy_url": "https://ntfy.sh",
  "ntfy_topic": "sliverd",
  "debug_mode": false,
  "fuso_orario": "Europe/Rome",
  "ora_legale": true,
  "notifiche_errori_abilitate": true,
  "meteo_esterno_url": "https://api.open-meteo.com",
  "meteo_esterno_latitudine": 37.6167,
  "meteo_esterno_longitudine": 15.1667,
  "sensore_url": "http://sensore.local",
  "relay_url": "http://relay.local",
  "database_path": "./data/termostato.db",
  "api_keys": ["sostituire-con-una-chiave-segreta"]
}
```

Forma del corpo di errore `400` (`ApiError`):

```json
{
  "timestamp": "2026-09-12T18:32:24Z",
  "status": 400,
  "error": "Bad Request",
  "message": "override_fine deve essere un istante futuro"
}
```

## Requisiti per il front-end

1. **Editing della forzatura.** Nella schermata/config della forzatura manuale, quando
   `override_attivo` è attivo, mostra un controllo opzionale "Fine forzatura" che consente di
   impostare **data e ora** (date-time picker in ora locale). Deve poter essere lasciato vuoto
   (forzatura a tempo indeterminato).

2. **Formato di invio.** Serializza il valore come stringa ISO locale **senza** offset/`Z`
   (`YYYY-MM-DDTHH:mm:ss`, i secondi possono essere `00`). Non convertire in UTC. Il valore
   corrisponde all'orario che l'utente vede sul proprio orologio locale.

3. **Coerenza col calendario.** Il campo usa lo stesso fuso/ora legale del calendario. Se la UI
   mostra già il calendario in ora locale, usa la stessa base temporale per il date-time picker.

4. **"Fino al prossimo cambio fascia" (opzionale, lato client).** Il backend NON offre
   un'opzione "termina all'inizio della prossima fascia". Se vuoi offrire questa comodità
   all'utente, calcola **lato front-end** l'istante di inizio della prossima fascia del
   calendario (usando `GET /config/calendario`, `fuso_orario` e `ora_legale`) e invialo come
   `override_fine` concreto. Questa è una funzionalità facoltativa della UI, non un requisito.

5. **Validazione preventiva.** Prima di inviare, verifica che, se `override_attivo` è attivo e
   `override_fine` è valorizzato, l'istante sia futuro; altrimenti mostra un messaggio inline e
   non inviare. Gestisci comunque il `400` di ritorno dal backend (fonte di verità) mostrando il
   `message` dell'`ApiError`.

6. **Refresh dello stato.** Poiché la forzatura può auto-disattivarsi lato backend alla
   scadenza, non assumere che lo stato locale della UI resti sincronizzato: ricarica la config
   (polling periodico o refresh) così che, alla scadenza, la UI mostri `override_attivo = false`
   e il ritorno all'operatività da calendario. Valuta di mostrare la fine impostata e,
   idealmente, un conto alla rovescia o un'indicazione "attiva fino alle HH:mm del GG/MM".

7. **Retrocompatibilità.** `override_fine` può essere assente/`null` in config esistenti: la UI
   deve gestire il valore nullo come "nessuna fine impostata".

## Criteri di accettazione

- Con override attivo, l'utente può impostare, modificare e rimuovere `override_fine` (data+ora,
  ora locale) e salvare via `PUT /config`.
- Il valore inviato è in formato ISO locale senza offset/`Z`.
- Un `override_fine` nel passato con override attivo è impedito lato UI e, se comunque inviato,
  l'errore `400` viene mostrato all'utente con il messaggio del backend.
- Con override attivo e `override_fine` vuoto, la forzatura resta a tempo indeterminato.
- Alla scadenza, dopo il refresh della config, la UI riflette `override_attivo = false` e il
  ripristino del calendario.
- Il valore `null` in `GET /config` è gestito senza errori.

## Riferimenti

- Specifica OpenAPI aggiornata: `docs/openapi.yaml` (schema `SystemConfiguration`, proprietà
  `override_fine`; endpoint `PUT /config` con risposta `400`).
- Specifiche funzionali: `docs/specifiche-funzionali.md`, sezione 3.2 (Override manuale) e
  requisito RF-48.
- Specifiche tecniche: `docs/specifiche-tecniche.md`, sezione 4.2.1 (Fine forzatura e
  auto-disattivazione) e tabella dei campi di configurazione.
