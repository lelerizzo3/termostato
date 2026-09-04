# Specifiche di sistema — Termostato intelligente

## 1. Descrizione generale

Il sistema gestisce un termostato domestico con programmazione settimanale. Legge periodicamente la temperatura ambiente e decide se attivare o disattivare la caldaia in base alla programmazione oraria configurata o a un'impostazione manuale di override. All'avvio, il sistema legge lo stato attuale del relay della caldaia direttamente dal dispositivo fisico, così da poter riprendere la logica di controllo senza assumere alcuno stato iniziale.

---

## 2. Calendario settimanale

### 2.1 Struttura

Il calendario è configurato tramite un file JSON e contiene esattamente **7 nodi**, uno per ogni giorno della settimana (lunedì–domenica).

### 2.2 Nodo giornaliero

Ogni nodo rappresenta un giorno e contiene un elenco di **intervalli di tempo**. Il numero di intervalli per giorno è variabile (da 0 a N).

### 2.3 Intervallo di tempo

Ogni intervallo specifica:

- **ora di inizio** — orario in cui l'intervallo diventa attivo
- **ora di fine** — orario in cui l'intervallo termina
- **temperatura target** — temperatura desiderata in gradi Celsius, con una cifra decimale (es. `20.5`)

Gli orari degli intervalli sono espressi in **UTC**. Il sistema confronta l'ora corrente in UTC con gli intervalli del calendario, eliminando qualsiasi ambiguità legata al cambio ora legale/solare.

### 2.4 Comportamento in assenza di intervalli

Se per il momento corrente non è definito nessun intervallo attivo nel calendario, la **caldaia rimane spenta**. Non viene applicata alcuna logica di isteresi: l'assenza di programmazione equivale esplicitamente a "nessun riscaldamento richiesto".

### 2.5 Esempio concettuale di struttura JSON

```
Settimana
├── Lunedì
│   ├── Intervallo 1: 06:00–08:00 → 20.5°C
│   ├── Intervallo 2: 12:00–14:00 → 19.0°C
│   └── Intervallo 3: 18:00–23:00 → 21.0°C
├── Martedì
│   └── ...
└── ...
```

---

## 3. Configurazione del sistema

La configurazione del sistema è separata dal calendario e contiene i seguenti parametri.

### 3.1 Soglia di attivazione caldaia (`soglia_attivazione`)

Valore numerico in gradi Celsius che rappresenta il **margine di tolleranza** sotto la temperatura target oltre il quale la caldaia viene accesa.

- Valori tipici: `0.2`, `0.3` (o altro valore analogo configurabile)
- Logica: se `temperatura_ambiente < temperatura_target - soglia_attivazione` → caldaia ON
- Logica: se `temperatura_ambiente >= temperatura_target` → caldaia OFF

Questo meccanismo evita accensioni e spegnimenti troppo frequenti (effetto isteresi).

### 3.2 Override manuale

| Parametro | Tipo | Descrizione |
|---|---|---|
| `override_attivo` | booleano | Se `true`, il calendario settimanale viene ignorato |
| `temperatura_override` | decimale | Temperatura target da mantenere quando l'override è attivo |

Quando `override_attivo` è `true`, il sistema ignora completamente il calendario e utilizza `temperatura_override` come unico riferimento, applicando comunque la soglia di attivazione.

### 3.3 Frequenza di polling (`intervallo_polling_secondi`)

Valore intero positivo che indica ogni quanti secondi il sistema esegue il ciclo di controllo: lettura della temperatura ambiente, calcolo della temperatura target e decisione sullo stato della caldaia.

- Unità: secondi
- Valore di esempio: `60`
- Il valore deve essere maggiore di zero; in caso di valore assente o non valido il sistema usa un default ragionevole (es. `60` secondi).

### 3.4 Soglia di errori consecutivi (`max_errori_consecutivi`)

Valore intero positivo che indica dopo quanti errori consecutivi delle API esterne il sistema deve mettere in sicurezza la caldaia spegnendola.

- Il contatore si azzera al primo ciclo di polling completato con successo
- Valore di esempio: `3`
- Non si applica agli errori di spegnimento caldaia, che vengono ritentati ad ogni ciclo senza limite (vedi sezione 4.3)

### 3.5 Retention dei log (`retention_log_giorni`)

Valore intero positivo che indica il numero di giorni per cui i record di log vengono conservati nel database. I record più vecchi vengono eliminati automaticamente dal processo di pulizia (vedi sezione 5.5).

- Unità: giorni
- Valore di esempio: `30`

### 3.6 Notifiche ntfy

| Parametro | Tipo | Descrizione |
|---|---|---|
| `ntfy_url` | stringa | URL base del servizio ntfy (es. `https://ntfy.sh`) |
| `ntfy_topic` | stringa | Topic ntfy a cui inviare i messaggi (valore corrente: `sliverd`) |
| `debug_mode` | booleano | Se `true`, invia messaggi informativi di accensione/spegnimento caldaia tramite ntfy |

Quando `debug_mode = false`, vengono inviati solo i messaggi di errore. Quando `debug_mode = true`, vengono inviati anche i messaggi informativi relativi alle azioni di accensione e spegnimento della caldaia.

### 3.7 Percorso del database (`database_path`)

Percorso del file di database **SQLite** utilizzato dal sistema per la persistenza dei log.

- Tipo: stringa (percorso su filesystem, assoluto o relativo alla directory di esecuzione)
- Valore di esempio: `./data/termostato.db`
- All'avvio, il sistema verifica l'esistenza del file indicato: se **non esiste**, lo crea (insieme alle eventuali directory intermedie) e inizializza lo schema; se **esiste**, lo utilizza così com'è.
- Il sistema non richiede alcun server di database esterno: l'intera persistenza è contenuta nel singolo file SQLite.
- Il parametro viene letto all'avvio e determina il datasource già aperto; per questo `database_path` è un parametro di **bootstrap** e non può essere modificato tramite `PUT /config` senza riavviare l'applicazione.

### 3.8 Autenticazione tramite API-key (`api_keys`)

La configurazione contiene un elenco di chiavi autorizzate:

| Parametro | Tipo | Descrizione |
|---|---|---|
| `api_keys` | elenco di stringhe | Insieme delle API-key abilitate per le richieste REST inbound |

Le richieste REST al backend devono presentare la chiave nell'header HTTP:

```http
X-API-Key: <api-key>
```

- Se il valore dell'header corrisponde a una delle chiavi presenti in `api_keys`, la richiesta viene elaborata normalmente.
- Se l'header è assente, vuoto o non corrisponde a una chiave configurata, il backend risponde con **HTTP 401 Unauthorized** e non invoca il controller.
- L'elenco vuoto è una configurazione **fail-closed**: nessuna richiesta REST autenticata può essere accettata finché non viene configurata almeno una chiave.
- La protezione si applica a tutti gli endpoint REST esposti dall'applicazione, inclusi gli endpoint Actuator e gli endpoint mock del profilo E2E.
- `api_keys` è persistito nel file JSON di configurazione e può essere aggiornato tramite `PUT /config` dopo l'autenticazione con una chiave già autorizzata.
- Le API-key del backend non vengono aggiunte alle chiamate outbound verso il servizio ntfy. Nel profilo mock vengono aggiunte solo alle chiamate interne verso i mock sensore/relay.

---

## 4. Integrazioni esterne

Il sistema non legge direttamente sensori hardware né comanda direttamente il relay della caldaia. Tutte le interazioni con il mondo fisico avvengono tramite **API REST esterne**. Il sistema dispone di due client REST dedicati: uno per il sensore di temperatura e uno per il relay della caldaia.

### 4.1 Client lettura temperatura

La temperatura ambiente viene acquisita tramite una chiamata a un'API REST esposta da un dispositivo esterno (es. sensore smart, microcontrollore con endpoint HTTP).

- Il client viene invocato ad ogni ciclo di polling
- Restituisce il valore di temperatura corrente in gradi Celsius
- L'endpoint di destinazione è configurabile

### 4.2 Client relay caldaia

Il client relay espone due operazioni verso la stessa API REST del dispositivo di controllo (es. relay smart, microcontrollore con endpoint HTTP):

**Operazione 1 — Lettura stato relay**
- Restituisce lo stato attuale del relay: acceso o spento
- Viene invocata **all'avvio dell'applicazione** per conoscere lo stato reale della caldaia prima di entrare nel ciclo di polling
- Può essere invocata anche durante il ciclo normale se necessario
- L'endpoint di destinazione è configurabile

**Operazione 2 — Comando accensione/spegnimento**
- Invia il comando di accensione o spegnimento al relay
- Viene invocata quando il sistema decide di cambiare lo stato della caldaia
- L'endpoint di destinazione è configurabile

> Lo stato della caldaia **non viene mai memorizzato in RAM** dal sistema. La fonte di verità è esclusivamente il relay fisico: ad ogni decisione che richiede lo stato attuale, esso viene letto tramite l'operazione di lettura del client relay.

### 4.3 Comportamento in caso di errore delle API

In caso di errore di comunicazione con le API esterne (timeout, errore HTTP, risposta non valida) il sistema:

1. **Invia una notifica** che descrive il tipo di errore, ad esempio:
   - "Impossibile leggere la temperatura dal sensore"
   - "Impossibile inviare il comando di accensione alla caldaia"
   - "Impossibile inviare il comando di spegnimento alla caldaia"

2. **Conta gli errori consecutivi** per ciascuna categoria di errore. Quando il contatore raggiunge la soglia configurata (`max_errori_consecutivi`), il sistema **mette in sicurezza la caldaia spegnendola**.

3. **Caso speciale — errore di spegnimento caldaia**: se l'errore riguarda proprio il comando di spegnimento, il sistema non applica la soglia ma tenta lo spegnimento **ad ogni ciclo di polling**, inviando una notifica di errore ad ogni tentativo fallito, finché l'operazione non ha successo.

### 4.4 Client notifiche (ntfy)

Le notifiche vengono inviate tramite il servizio **[ntfy](https://ntfy.sh)**, che recapita i messaggi all'app ntfy su iPhone. Il client è strutturato in modo analogo ai client del relay e del termometro.

Il client invia messaggi di due tipi:

**Messaggi di errore**
- Inviati ogni volta che si verifica un errore di comunicazione con le API esterne
- Inviati sempre, indipendentemente dal valore di `debug_mode`
- Priorità: alta (errore)
- Esempio: "Impossibile leggere la temperatura dal sensore"

**Messaggi informativi** *(solo se `debug_mode = true`)*
- Inviati ogni volta che la caldaia viene accesa o spenta
- Priorità: normale (info)
- Esempio: "Caldaia accesa — temperatura rilevata 19.3°C, target 20.5°C"
- Esempio: "Caldaia spenta — temperatura rilevata 20.6°C, target 20.5°C"

Parametri di configurazione relativi alle notifiche:

| Parametro | Tipo | Descrizione |
|---|---|---|
| `ntfy_url` | stringa | URL base del servizio ntfy (es. `https://ntfy.sh`) |
| `ntfy_topic` | stringa | Topic ntfy a cui inviare i messaggi (valore corrente: `sliverd`) |
| `debug_mode` | booleano | Se `true`, abilita l'invio dei messaggi informativi di accensione/spegnimento |

---

## 5. Database e log

### 5.0 Motore di persistenza

Il sistema utilizza **SQLite** come motore di persistenza, con l'intero database contenuto in un unico file su filesystem il cui percorso è configurabile tramite il parametro `database_path` (vedi sezione 3.7). Questa scelta consente di eseguire l'applicativo su qualsiasi macchina dotata del solo runtime Java, senza dipendere da un server di database esterno.

**Inizializzazione automatica all'avvio:**

1. Il sistema legge `database_path` dalla configurazione.
2. Se il file non esiste, viene creato (comprese le directory intermedie mancanti) e lo schema (tabelle di log di polling e log errori) viene inizializzato automaticamente.
3. Se il file esiste già, viene aperto e utilizzato senza reinizializzare i dati esistenti.

### 5.1 Scopo

Ad ogni ciclo di polling il sistema registra su database uno snapshot dello stato del termostato. Questi dati consentono di tracciare l'andamento della temperatura e il comportamento della caldaia nel tempo.

### 5.2 Struttura del record di log

| Campo | Tipo | Obbligatorio | Descrizione |
|---|---|---|---|
| `data_ora` | timestamp | sì | Data e ora esatta del ciclo di polling |
| `caldaia_accesa` | booleano | sì | `true` se la caldaia era accesa al momento del rilevamento |
| `temperatura_rilevata` | decimale | sì | Temperatura ambiente letta tramite API, in °C con una cifra decimale |
| `temperatura_target` | decimale | sì | Temperatura target calcolata (da calendario o override) |
| `override_attivo` | booleano | sì | `true` se in quel momento era attivo l'override manuale |
| `temperatura_override` | decimale | no | Temperatura di override impostata; valorizzato solo se `override_attivo = true` |

### 5.3 Frequenza di scrittura

Un record viene scritto **ad ogni ciclo di polling**, indipendentemente dal fatto che lo stato della caldaia sia cambiato o meno.

### 5.4 Tabella log errori

In aggiunta alla tabella di log principale, il sistema mantiene una tabella dedicata ai soli eventi di errore. Un record viene scritto ogni volta che si verifica un errore di comunicazione con le API esterne.

| Campo | Tipo | Obbligatorio | Descrizione |
|---|---|---|---|
| `data_ora` | timestamp | sì | Data e ora in cui si è verificato l'errore |
| `tipo_errore` | stringa | sì | Descrizione del tipo di errore (es. "Impossibile leggere la temperatura", "Impossibile inviare comando di accensione caldaia", "Impossibile inviare comando di spegnimento caldaia") |
| `caldaia_accesa` | booleano | no | Stato della caldaia al momento dell'errore, se disponibile |
| `temperatura_rilevata` | decimale | no | Ultima temperatura letta, se disponibile |
| `num_errori_consecutivi` | intero | sì | Numero di errori consecutivi raggiunto al momento di questo evento |

### 5.5 Conservazione dei dati

La durata di conservazione dei log è configurabile tramite il parametro `retention_log_giorni`, che indica il numero di giorni oltre i quali i record vengono eliminati. La pulizia si applica sia alla tabella dei log di polling che alla tabella dei log di errore.

Un processo dedicato viene eseguito **ogni ora** e cancella tutti i record con `data_ora` anteriore a `now - retention_log_giorni`.

---

## 7. API REST esposte

L'applicazione espone una serie di endpoint REST utilizzati da un front-end per la gestione e il monitoraggio del termostato.

### 7.0 Autenticazione delle API REST

Tutti gli endpoint REST del backend richiedono l'header `X-API-Key` con una chiave presente nella configurazione `api_keys`. Una richiesta senza chiave o con chiave non autorizzata restituisce HTTP `401 Unauthorized`.

### 7.1 Configurazione

| Metodo | Endpoint | Descrizione |
|---|---|---|
| `GET` | `/config` | Restituisce la configurazione corrente del sistema (tutti i parametri) |
| `PUT` | `/config` | Aggiorna la configurazione del sistema |
| `GET` | `/config/calendario` | Restituisce il calendario settimanale |
| `PUT` | `/config/calendario` | Aggiorna il calendario settimanale |

### 7.2 Log di polling

| Metodo | Endpoint | Descrizione |
|---|---|---|
| `GET` | `/log` | Restituisce i record di log di polling |

**Parametri query opzionali:**

| Parametro | Tipo | Descrizione |
|---|---|---|
| `da` | data (UTC) | Inizio del range di date (incluso) |
| `a` | data (UTC) | Fine del range di date (incluso) |

Se nessun parametro viene passato, il servizio restituisce i log del **giorno corrente** in UTC. Se viene passato solo `da`, il limite superiore è la fine del giorno corrente.

### 7.3 Log di errore

| Metodo | Endpoint | Descrizione |
|---|---|---|
| `GET` | `/log/errori` | Restituisce i record della tabella log errori |

**Parametri query opzionali:**

| Parametro | Tipo | Descrizione |
|---|---|---|
| `da` | data (UTC) | Inizio del range di date (incluso) |
| `a` | data (UTC) | Fine del range di date (incluso) |

Se nessun parametro viene passato, il servizio restituisce gli errori del **giorno corrente** in UTC. Se viene passato solo `da`, il limite superiore è la fine del giorno corrente.

---

## 8. Logica di controllo

### 8.1 Avvio del sistema

All'avvio, prima di entrare nel ciclo di polling, il sistema:

1. Legge lo stato attuale del relay tramite il client relay (operazione di lettura stato)
2. Utilizza tale stato come punto di partenza per la logica di controllo

### 8.2 Sorgente della temperatura target

1. Se `override_attivo = true` → usa `temperatura_override`
2. Altrimenti → cerca l'intervallo attivo nel calendario per il giorno e l'ora correnti
3. Se nessun intervallo è attivo → **caldaia spenta** (nessuna temperatura target)

### 8.3 Decisione di accensione caldaia

Lo stato corrente della caldaia utilizzato nella logica di isteresi (zona neutra) viene letto dal relay fisico, non da una variabile interna.

| Condizione | Stato caldaia |
|---|---|
| `temperatura_ambiente < temperatura_target - soglia_attivazione` | ON |
| `temperatura_ambiente >= temperatura_target` | OFF |
| Tra i due valori (zona neutra) | invariato — stato letto dal relay |

---

## 9. Requisiti funzionali

| ID | Requisito |
|---|---|
| RF-01 | Il sistema legge la configurazione del calendario da file JSON |
| RF-02 | Il calendario contiene esattamente 7 nodi, uno per giorno della settimana |
| RF-03 | Ogni giorno può avere un numero variabile di intervalli orari |
| RF-04 | Ogni intervallo definisce ora di inizio, ora di fine e temperatura target con una cifra decimale |
| RF-05 | Il sistema legge la soglia di attivazione dalla configurazione |
| RF-06 | Il sistema supporta la modalità override tramite i parametri `override_attivo` e `temperatura_override` |
| RF-07 | Quando l'override è attivo, il calendario viene ignorato |
| RF-08 | La caldaia viene accesa quando la temperatura ambiente scende sotto `temperatura_target - soglia_attivazione` |
| RF-09 | La caldaia viene spenta quando la temperatura ambiente raggiunge `temperatura_target` |
| RF-10 | In assenza di intervallo attivo nel calendario (e senza override), la caldaia rimane spenta |
| RF-11 | La frequenza del ciclo di controllo è configurabile tramite il parametro `intervallo_polling_secondi` |
| RF-12 | La temperatura ambiente viene letta tramite un client REST dedicato |
| RF-13 | L'accensione e lo spegnimento della caldaia avvengono tramite il client relay (operazione di comando) |
| RF-21 | Il client relay espone anche un'operazione di lettura stato che restituisce lo stato attuale del relay |
| RF-22 | All'avvio il sistema legge lo stato del relay tramite l'operazione di lettura, senza assumere alcuno stato iniziale |
| RF-23 | Lo stato della caldaia non viene memorizzato in RAM; la fonte di verità è esclusivamente il relay fisico |
| RF-14 | Gli endpoint delle API esterne sono configurabili |
| RF-15 | In caso di errore delle API esterne il sistema invia una notifica che descrive il tipo di errore |
| RF-16 | Il sistema conta gli errori consecutivi per categoria; al raggiungimento della soglia `max_errori_consecutivi` spegne la caldaia per sicurezza |
| RF-17 | Se l'errore riguarda il comando di spegnimento caldaia, il sistema ritenta lo spegnimento ad ogni ciclo di polling e invia notifica di errore ad ogni tentativo fallito |
| RF-18 | Ad ogni ciclo di polling il sistema scrive un record di log su database |
| RF-19 | Il record di log contiene: `data_ora`, `caldaia_accesa`, `temperatura_rilevata`, `temperatura_target`, `override_attivo` e, se l'override è attivo, `temperatura_override` |
| RF-20 | Il sistema mantiene una tabella separata per i log di errore, con i campi: `data_ora`, `tipo_errore`, `caldaia_accesa` (se disponibile), `temperatura_rilevata` (se disponibile), `num_errori_consecutivi` |
| RF-24 | Gli orari degli intervalli del calendario sono espressi in UTC |
| RF-25 | Il sistema confronta l'ora corrente in UTC con gli intervalli del calendario |
| RF-26 | La durata di conservazione dei log è configurabile tramite il parametro `retention_log_giorni` |
| RF-27 | Un processo dedicato viene eseguito ogni ora e cancella i record di log più vecchi di `retention_log_giorni` giorni, sia dalla tabella dei log di polling che da quella dei log di errore |
| RF-28 | Le notifiche vengono inviate tramite il servizio ntfy verso il topic configurato (`ntfy_topic`) |
| RF-29 | I messaggi di errore vengono inviati sempre, indipendentemente dal valore di `debug_mode` |
| RF-30 | Se `debug_mode = true`, il sistema invia tramite ntfy un messaggio informativo ad ogni accensione e spegnimento della caldaia |
| RF-31 | L'applicazione espone un endpoint REST per leggere la configurazione corrente |
| RF-32 | L'applicazione espone un endpoint REST per aggiornare la configurazione |
| RF-33 | L'applicazione espone un endpoint REST per leggere e aggiornare il calendario settimanale |
| RF-34 | L'applicazione espone un endpoint REST per consultare i log di polling, con filtro opzionale per range di date; in assenza di parametri restituisce i log del giorno corrente |
| RF-35 | L'applicazione espone un endpoint REST per consultare i log di errore, con filtro opzionale per range di date; in assenza di parametri restituisce gli errori del giorno corrente |
| RF-36 | Il sistema utilizza un database SQLite su file, il cui percorso è configurabile tramite il parametro `database_path` |
| RF-37 | All'avvio il sistema verifica l'esistenza del file di database: se non esiste lo crea (con le directory intermedie) e ne inizializza lo schema; se esiste lo utilizza senza reinizializzare i dati |
| RF-38 | La configurazione contiene l'elenco `api_keys` delle chiavi autorizzate per le richieste REST inbound |
| RF-39 | Ogni richiesta REST deve presentare nell'header `X-API-Key` una chiave presente in `api_keys`; in caso di chiave assente o non valida il backend restituisce HTTP 401 Unauthorized |

---

## 10. Requisiti non funzionali

| ID | Requisito |
|---|---|
| RNF-01 | La configurazione deve essere modificabile senza riavviare il sistema |
| RNF-02 | Le temperature sono espresse in gradi Celsius con una cifra decimale |
| RNF-03 | Il sistema deve essere robusto in caso di configurazione mancante o malformata |
| RNF-04 | Il sistema deve gestire in modo controllato gli errori di comunicazione con le API esterne |
| RNF-05 | L'applicativo è distribuito come singolo jar eseguibile da console e richiede sulla macchina di destinazione unicamente un runtime Java, senza server di database esterni né container |

---

## 11. Aspetti aperti (da definire)

Nessun aspetto aperto rilevante al momento. Le specifiche sono complete.
