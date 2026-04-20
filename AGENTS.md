# AGENTS.md

Vigo Skole benyttes til å overføre data om avgangselever fra ungdomsskolenes skoleadministrasjonssystem
(SAS) til Vigo. Overføringene gjøres av bemyndigede personer ved ungdomsskolene. Innsendinger til
Vigo Skole aksepteres kun når valideringen skjer uten feil. Valideringen kan gi advarsler, men 
innsendingen er likevel godkjent. Ved godkjent innsending for et skoleår kan innsending av en hvis type
ikke gjøres på nytt for det skoleåret.

Etter frist for innlevering vil inntakskontoret for videregående utdanning laste dataene inn i Vigo.

Dette er en fake implementasjon av Vigo Skole for å kunne verifisere overføring av data om 
avgangselever fra ungdomsskolenes skoleadministrasjonssystem (SAS) til Vigo Skole. Ekte Vigo Skole
har ikke et API, men denne tjenesten skal demonstrere hvordan et slikt API burde fungere.

For å kunne verifisere at data om avgangselever blir korrekt overført fra SAS til Vigo Skole, er det
nødvendig å kunne inspisere overføringene og valideringsresultater for disse. Brukere skal derfor kunne
logge inn i Vigo Skole og inspisere hva som har blitt lastet opp og valideringsresultater.
For hver overføring skal det også vises metadata om når overføringen skjedde og eventuelt om en
leverandør (supplier) gjorde opplastningen på vegne av en ungdomsskole (hentes fra Maskinporten token).

Over tid er det snakk
om tre overføringer for avgangselever: 1. liste over alle avgangselever, 2. tentamentskarakterer og 3.
karakterer for eksamen. Per nå skal kun støtte for den første overføringen implementeres (liste over
avgangselever).

## Overføring av liste over avgangselever
- Bruker med rettigheter til å utføre innsending logger inn ungdomsskolens SAS og initierer innsending. 
- Innsending kan kun skje innenfor et tidsrom satt av inntakskontoret for videregående utdanning.
- Innsending er for inneværende skoleår.
- Overføringen skal bruke HTTP POST med JSON-LD formatert body/melding.
- Per elev skal meldingen inneholde følgende informasjon:
  - Klassekode
  - Fødselsnr
  - Elevnavn

## Overføring av tentamentskarakterer og eksamenskarakterer
- Bruker med rettigheter til å utføre innsending logger inn ungdomsskolens SAS og initierer innsending. 
- Innsending kan kun skje innenfor et tidsrom satt av inntakskontoret for videregående utdanning.
- Innsending er for inneværende skoleår
- Innsending stenges etter suksessfull innsending (dvs. uten feil, men kan inneholde advarsler)
- Overføringen skal bruke HTTP POST med JSON-LD formatert body/melding. Denne bodyen/meldingen
  må inneholde dataene som tidligere ble uttrykt med radene Elev ($EL), Elevkurs ($EK),
  Kompetansebevis-linje ($KL) og Vitnemålsmerknader ($VM) i overføringsformatet beskrevet
  her: https://ist.guidecloud.se/2136.guide?pID=12 og https://ist.guidecloud.se/media/20230214/2136/Grensesnitt_SAS_3.pdf.

- Det gamle Filformatet til IST er også dokumentert her: 
START (TOF)
#	Kolonnenavn 	Format	Obl	Kommentar	JSON
1	Linje-identifikator	x(3)	J	TOF	LINJEID
2	Versjonsnr	x(7)	J	Grensesnittets versjonsnr	VERSJON
3	Fildato	9(8)	N	Dato da fila ble laget (ÅÅÅÅMMDD)	FILDATO
4	Melding	x(80)	N	Melding fra avgiversystem.  Skal primært brukes til å identifisere avgiver.	MELDING
5	Fylkesnummer	9(2)	J	Skolens/filas fylkesnummer. Må stemme overens med fylkestilhørigheten til brukeren som kjører VIGO.	FYLKESNR
6	Skolenummer	9(5)	N	Utfylt hvis hele fila gjelder kun én skole	SKOLENR
7	Avgiver-ID	X(80)	N	Avgiversystemets egendefinerte identifikasjon av sendingen	AVGIVERID
Elev ($EL)
#	Kolonnenavn 	Format	Obl	Over skriv?	Kommentar	SSB	Rhb	JSON
1	Linje-identifikator	x(3)	J		$EL			LINJEID
2	Fødselsnr	9(11)	J		Må ikke være ugyldig ihht VIGOs fnr-sjekk. Det gis advarsel hvis fnr er fiktivt.	R.1
E.1
V.1	A01	FNR
3	Elevnavn	x(100)	N	N	Importeres hvis blank i VIGO. Navnet importeres slik det ligger på fila.
Ønsket rekkefølge: Etternavn Fornavn Mellomnavn
Ønsket format: Duck Donald, McDuck Skrue	R.8
E.8		ELEVNAVN
4	Gateadresse (bosted)	x(60)	N	J, logges	Ønsket format: Andebyveien 1
Importeres i VIGO hvis feltet er utfylt.			GATE2
5	Postnr (bosted)	9(4)	N	J, logges	Må finnes i VIGOs poststeds-tabell. Se VIGO Kodeverksbase – tabell Poststeder. Importeres i VIGO hvis feltet er utfylt.			POSTNR2
6	Kommunenr (bosted)	9(4)	N	J, logges	Må finnes i VIGOs kommune-tabell. Se VIGO Kodeverksbase – tabell Kommuner. Importeres i VIGO hvis feltet er utfylt.			KOMMNR2
7	<ikke i bruk>							
8	Morsmålskode	x(3)	N	J, logges	Det gis advarsel hvis koden ikke finnes i VIGOs morsmåls-tabell. Se VIGO Kodeverksbase – tabell Morsmål.			MORSMKOD
9	Fritekstfelt	x(60)	N	-	Feltet benyttes av WIS/Udir for å hente inn nødvendig informasjon fra de private videregående skolene. WIS/Udir gir nærmere regler om bruk av feltet.
Data sendes ikke videre fra WIS til Vigo.			FRITEKST


Elevkurs ($EK)
#	Kolonnenavn 	Format	Obl	Over skriv?	Kommentar	SSB	Rhb	JSON
1	Linje-identifikator	x(3)	J		$EK			LINJEID
2	Fødselsnr	9(11)	J		Må ikke være ugyldig ihht VIGOs fnr-sjekk. Det gis advarsel hvis fnr er fiktivt.	R.1
E.1	A01	FNR
3	Skoleår	9(8)	J		Må finnes i VIGOs skoleår-tabell	R.2
E.2		SKOLEAR
4	Skolenr	9(5)	J		Må finnes som en aktiv skole i VIGOs skole-tabell. Se VIGO Kodeverksbase – tabell Skoler. Må være på samme nivå som programområde (gsk/vg).	R.3
E.3	A06	SKOLENR
5	Programområdekode	x(10)	J		Må finnes i VIGOs programområde-tabell. Se VIGO Kodeverksbase – tabell Programområder. Må være på samme nivå som skole (gsk/vg).	R.4
E.4	A03	KURSKODE
6	Klassekode	x(40)	N	J, logges	Blankes i VIGO hvis blank på fila			KLASSE
7	Startdato	9(8)	J	J, logges	ÅÅÅÅMMDD.
Gjelder vgo:
Elevens startdato på programområdekoden (#5). Med unntak av privatister skal alle på vgo ha utfylt startdato. Advarsel hvis blank på fila.	R.11	B02	STARTDAT
8	Avbruddsdato	9(8)	N	J, logges	ÅÅÅÅMMDD. Brukes hvis eleven har avvikende avslutning ifht standard for tilbudet. Blankes i VIGO hvis blank på fila.	R.12	B04	SLUTTDAT
9	Avbruddsårsak	x(8)	N	J	Må finnes i VIGOs årsaks-tabell. Se VIGO Kodeverksbase – tabell Årsakskoder – Type S. Blankes i VIGO hvis blank på fila.		B06	FRAFAARS
10	Deltidselev?	x(1)	N	J	J,N			DELK
11	Bevistype	x(2)	N	J	VM = Vitnemål
KB = Kompetansebevis
Se VIGO Kodeverksbase – tabell Div. variabler registreringshåndboken - Bevistype.
Ved skoleårets slutt skal det leveres VM eller KB.	R.22	B22	BEVISTYP
12	Fullførtkode	x(1)	N	J	Gyldige koder fra videregående skoler:
B, I, M, A, H, L, K og S
Se VIGO Kodeverksbase – tabell Div. variabler registreringshåndboken - Fullførtkoder.
For nærmere forklaring se registreringshåndboken.
Blank fra SAS gir X i VIGO.

Gyldige koder fra grunnskoler:
B = Fullført og bestått
S = Sluttet/avbrutt	R.13	B21	FULLFKOD
13	Antall dager fravær	9(3)	N	J	Antall hele dager fravær som skal på vitnemål/kompetansebevis. Dersom eleven ikke har fravær skal det leveres 0.	R.18	B24	FRAVDAG
14	Antall timer fravær	9(4)	N	J	Antall hele timer fravær (i tillegg til dager) som skal på vitnemål/kompetansebevis. Dersom eleven ikke har fravær skal det leveres 0.	R.19	B24	FRAVTIME
15	Ordenskarakter	x(2)	N	J	G = Godt, NG = Nokså godt, LG = Lite godt		B25	KARORD
16	Oppførselskarakter	x(2)	N	J	G = Godt, NG = Nokså godt, LG = Lite godt		B25	KARATF
17	Rettstype – importert	x(1)	N	J	U,V,I. Importeres kun i VIGO hvis J i variabel «Voksen i opplæringstilbud tilpasset voksne?», og lagres da i VIGOs kolonne for importert rettstype.	R.14
E.13		RETT2IMP
18	Voksen i opplærings-tilbud tilpasset voksne?	x(1)	N	J	J,N. J kan utløse elevstatus V.
Angir om den voksne har rett til videregående opplæring for voksne etter opplæringsloven § 18-3.			VOKSEN
19	Målform (norsk hovedmål)	x(1)	N	J	B = Bokmål, N = Nynorsk, S = Samisk	R.24	B27	MALFORM
20	Fritekstfelt	x(60)	N	-	Importeres ikke i VIGO. Feltet benyttes av WIS/Udir for å hente inn nødvendig informasjon fra de private videregående skolene. WIS/Udir gir nærmere regler om bruk av feltet. Data sendes ikke videre fra WIS til Vigo.			FRITEKST
21	Antall dager fravær totalt	9(3)	N	J	Totalt antall hele dager fravær før evt. fratrekk som ikke skal ut på dokumentasjon. Dersom eleven ikke har fravær skal det leveres 0.	R.27		FRAVDAGT
22	Vedtak om særskilt språkopplæring	x(1)	N	J	J,N. Angir om eleven har vedtak om særskilt språkopplæring. Gjelder videregående opplæring.	R.29		SERSPRAK
23	Antall timer fravær totalt	9(4)	N	J	Totalt antall hele timer fravær (i tillegg til dager) før evt. fratrekk som ikke skal ut på dokumentasjon. Dersom eleven ikke har fravær skal det leveres 0.	R.28		FRAVTIMET
24	Rett til påbygging	X(1)	N	J	J,N. Angir om den voksne har rett til påbygging til generell studiekompetanse etter opplæringsloven § 18-7. Importeres kun hvis J i «Voksen i opplæringstilbud tilpasset voksne?».	E.19		RETTPB


Kompetansebevis-linje ($KL)
#	Kolonnenavn 	Format	Obl	Kommentar	SSB	Rhb	JSON
1	Linje-identifikator	x(3)	J	$KL		A01	LINJEID
2	Fødselsnr	9(11)	J	Må ikke være ugyldig ihht VIGOs fnr-sjekk. Det gis advarsel hvis fnr er fiktivt.	R.1
E.1		FNR
3	Skoleår	9(8)	J	Må finnes i VIGOs skoleår-tabell	R.2
E.2	A06	SKOLEAR
4	Skolenr	9(5)	J	Må finnes som en aktiv skole i VIGOs skole-tabell. Se VIGO Kodeverksbase – tabell Skoler.	R.3
E.3	A03	SKOLENR
5	Programområdekode	x(10)	J	Må finnes i VIGOs programområde-tabell. Se VIGO Kodeverksbase – tabell Programområder.	R.4
E.4	A04	KURSKODE
6	Fagkode	x(7)	J	Må finnes i VIGOs fag-tabell. Se VIGO Kodeverksbase – tabell Fag. Må være på samme nivå som programområde (gsk/vg/IB).	RF.5
EF.5	A05	FAGKODE
7	Karakter termin 1	x(2)	N	Må finnes i VIGOs karakter-tabell som en aktiv karakter. Se VIGO Kodeverksbase – tabell Karakterer.	RF.11	B26	KART1
8	Karakter termin 2	x(2)	N	Må finnes i VIGOs karakter-tabell som en aktiv karakter. Se VIGO Kodeverksbase – tabell Karakterer.
Gjelder fag som i VIGO kodeverk er merket som «Fellesfag uten stp-kar».	RF.12	B26	KART2
9	Karakter standpunkt	x(2)	N	Må finnes i VIGOs karakter-tabell som en aktiv karakter. Se VIGO Kodeverksbase – tabell Karakterer.	RF.13	B26	KARSTP
10	Karakter skriftlig eksamen	x(2)	N	Må finnes i VIGOs karakter-tabell som en aktiv karakter. Se VIGO Kodeverksbase – tabell Karakterer.	RF.16	B26	KARSKR
11	Karakter muntlig eksamen	x(2)	N	Må finnes i VIGOs karakter-tabell som en aktiv karakter. Se VIGO Kodeverksbase – tabell Karakterer.	RF.19	B26	KARMUN
12	Karakter annen eksamen	x(2)	N	Må finnes i VIGOs karakter-tabell som en aktiv karakter. Se VIGO Kodeverksbase – tabell Karakterer.	RF.22	B26	KARANN
13	Eksamensform	x(2)	N	Skal ha verdi hvis eleven er trukket ut til eksamen, og eksamens-karakteren er ulik GK. Se VIGO Kodeverksbase – tabell Eksamensformer.		B15	EKSTYPE
14	Skolenr 2	9(5)	N	Alternativt skolenr. Brukes hvis faget er tatt ved en annen skole enn hovedskolen. Må finnes i VIGOs skole-tabell. Se VIGO Kodeverksbase – tabell Skoler.	RF.24		SKOLENR2
15	Skoleår 2	9(8)	N	Alternativt skoleår. Brukes hvis faget er tatt et annet skoleår enn hovedskoleåret. Må finnes i VIGOs skoleår-tabell.	RF.23		SKOLEAR2
16	Fagstatus	x(1)	N	Kompetansebevis-linjas status.
E = Elev
A = Individuelt tilrettelagt opplæring
B = Mer opplæring
N = Nettundervisning
U = Utenlandsk utvekslingselev i Norge
V = Voksen
O = Oppdragsundervisning
S = Sluttet
F = Fritatt
P = Privatist
R = Realkompetansevurdert
M = Fagopplæring i skole
G = Godkjent
Se VIGO Kodeverksbase – tabell Div. variabler registreringshåndboken - Fagstatus.	RF.10
EF.10	B07	KLSTATUS
17	Startdato	9(8)	N	ÅÅÅÅMMDD.
Gjelder vgo:
Elevens startdato på fagkoden (#6). Med unntak av privatister skal alle på vgo ha utfylt startdato. Fag som tildeles eleven før skolen starter skal ha startdato = første skoledag.
Gjelder gr.sk:
Blankes i VIGO hvis blank på fila.		B03	STARTDAT
18	Avbruddsdato	9(8)	N	ÅÅÅÅMMDD. Brukes hvis eleven har avvikende avslutning ifht standard for tilbudet. Ingen fagkode (#6) skal ha avbruddsdato før første skoledag. Blankes i VIGO hvis blank på fila.		B05	SLUTTDAT
19	Elevtimer	9(4)	N	Skal kun registreres på gitte fagkoder – se registrerings-håndboken. Importeres kun i VIGO hvis J i variabel «Elevtimer er grunnlag for kurs%».	RF.26
EF.13	B09	ELEVTIMER
20	Termin	x(1)	N	H = Høst, V = Vår
Elever (inkl voksne)/privatister som får H2-, standpunkt- eller eksamenskarakter om høsten skal ha H. Når karakteren gis på våren skal det være V.			EKSTERM
21	Fagmerknad - kode	x(5)	N	Må finnes i VIGOs tabell med fagmerknads-koder. Se VIGO Kodeverksbase – tabell Fagmerknader. Blankes i VIGO hvis blank på fila.	RF.30	B16	FAMKODE
22	Fagmerknad – parameter	x(40)	N	Ingen kontroll i VIGO. Blankes i VIGO hvis blank på fila.  Skal kun fylles ut hvis teksten til fagmerknaden krever en parameter.		B17	FAMVAR
23	Karakterstatus	x(1)	N	Kode for karakterer som blir endret.
N = Ny
U = Utsatt
S = Særskilt prøve
E = Endring pga feilføring
K = Klage på karakteren
Se VIGO Kodeverksbase – tabell Div. variabler registreringshåndboken - Karakterstatus.	RF.31	B23	KARSTAT
24	Fritekstfelt	x(60)	N	Importeres ikke i VIGO. Feltet benyttes av WIS/Udir for å hente inn nødvendig informasjon fra de private videregående skolene. WIS/Udir gir nærmere regler om bruk av feltet.
Data sendes ikke videre fra WIS til Vigo.			FRITEKST
25	Realkompetansevurdert dato	9(8)	N	ÅÅÅÅMMDD. Dato for når faget er realkompetansevurdert. Dersom det ligger dato i dette feltet og samtidig fagstatus V og karakter i standpunkt/eksamen, viser det at kandidaten tidligere har vært realkompetansevurdert med resultat IG.	RF.32		RKVDATO


Vitnemålsmerknader ($VM)
#	Kolonnenavn 	Format	Obl	Kommentar	SSB	Rhb	JSON
1	Linje-identifikator	x(3)	J	$VM			LINJEID
2	Fødselsnr	9(11)	J	Må ikke være ugyldig ihht VIGOs fnr-sjekk. Det gis advarsel hvis fnr er fiktivt.	R.1
A01	FNR
3	Skoleår	9(8)	J	Må finnes i VIGOs skoleår-tabell	R.2		SKOLEAR
4	Skolenr	9(5)	J	Må finnes som en aktiv skole i VIGOs skole-tabell. Se VIGO Kodeverksbase – tabell Skoler.	R.3
A06	SKOLENR
5	Programområdekode	x(10)	J	Må finnes i VIGOs programområde-tabell. Se VIGO Kodeverksbase – tabell Programområder.	R.4
A03	KURSKODE
6	Vitnemålsmerknad - kode	x(5)	J	Må finnes i VIGOs tabell med vitnemålsmerknads-koder. Se VIGO Kodeverksbase – tabell Vitnemålsmerknader.	RV.5	B18	VMMKODE
7	Vitnemålsmerknad - parameter 1	x(255)	N	Blankes i VIGO hvis blank på fila.  Skal kun fylles ut hvis teksten til vitnemålsmerknaden krever minst 1 parameter.		B19	VMMVAR1
8	Vitnemålsmerknad - parameter 2	x(255)	N	Blankes i VIGO hvis blank på fila. Skal kun fylles ut hvis teksten til vitnemålsmerknaden krever 2 parametre.		B19	VMMVAR2


## Krav til Vigo Skole API
- Bruk Maskinporten for maskin-til-maskin-kommunikasjon. Autentiser JWT access token fra Maskinporten.
- REST API med JSON-LD format.
- Innsending med HTTP POST.
- En innsending skal inneholde data for alle avgangselever ved en ungdomsskole.
- Perioden hvor innsending er tillat og mulig er konfigurert med default-periode i application.yml og kan styres gjennom web appen (fra og til dato-range)
- Tokens fra Maskinporten inneholder avsenderens organisasjonsnummer. For innsendingene vil dette være
  organisasjonsnummeret til ungdomsskolen som sender data. Tjenesten skal verifisere at dette 
  organisasjonsnummeret faktisk tilhører en ungdomsskole. Denne verifikasjonen skjer ved å sjekke at 
  organisasjonsnummeret representerer en ungdomsskole i VIGO Kodeverk.
- Sjekk av organisasjonsnummeret mot VIGO Kodeverk gjøres med GET til https://kodeverk.vigo.no/api/schools?page=0&size=10 med en body som dette: [{"key":"orgNr","value":"974603268","operation":"EQUAL"}]
  I dette eksemplet er 974603268 organisasjonsnummeret. Det må sjekkes at resultatet inneholder en entry i
  "content" propertyen som har key "orgNr" og value "974603268", samt at "type" er lik "G". Ved match, ta
  vare på "name" (ungdsomsskolens navn), "municipalityNr" (kommunenummer), "countyNr" (fylkesnummeret) og
  "number" (skolenummeret). Disse assosieres med opplastningen og brukes[fint-ontology.ttl](../../../Documents/Novari/fint-ontology.ttl) ved fremvisning av opplastning
  til innloggede brukere i Vigo Skole.
- HTTP respons til klienten som laster opp:
  - HTTP 201 kun hvis innsendingen er godkjent uten feil. Advarsler er tillatt.
  - Hvis innsendingen for et skoleår er mottatt og akseptert, skal påfølgende innsendinger avvises med HTTP 403 og body med feilmelding.
  - HTTP 4** hvis innsendingen inneholder valideringsfeil eller er tom (ingen elever) eller ugyldig payload. Body med feilmelding per elev.
- Fødselsnummer per elev skal være gyldig fødselsnummer, D-nummer eller fiktivt fødselsnummer.
- Regler for fiktivt fødselsnummer:
  Fødselsdato består av gyldig dato + 40 dager (for eksempel 011285 blir til 411285).
  Personnummer består av 99 + kjønnshenvisning (partall = jenter, oddetall = gutter) + fylkesnummer (ledende null - eks for Akershus 02). Dersom det allerede er opprettet en person i Vigo med 99 kan dette erstattes med 98, 97 osv.
  Eksempel jente fra Akershus: 42059199202 (født 020591 og hjemmehørende i fylke 02)
  Eksempel gutt fra Troms: 55049199119 (født 150491 og hjemmehørende i fylke 19)
- JSON-LD formaterte meldinger:
  - Anta at ontologien har adresse https://novari.no/ontology/fint.ttl
  - En foreløpig testversjon av ontologien er tilgjengelig i filen [fint-ontology.ttl](../../../Documents/Novari/fint-ontology.ttl)
  - Person-data for En elev er en fint:felles/person.
  - Navn på elev er property fint:felles/person/navn med type fint:felles/komplekse-datatyper/personnavn
  - Finn på nye properties for klassekode og andre manglende properties og typer i ontologien.

## Web App
Web appen skal gjøre at fylkeskommune-brukere kan inspisere opplastningene som er gjort til Vigo Skole.
- Innlogging med ID-porten.
- Appen skal vise hvem som er pålogget og action for å logge ut.
- Appen skal være bygd opp etter følgende hierarki/navigasjon: Skoleår > Fylkeskommune (fylkesnummer og fylkeskommunenavn) > Ungdomsskole (skolenummer, skolenavn og status innsending) > Innsendinger > Innsending
- Siden for en innsending skal vise alle dataene som er lastet opp og valideringsresultater.
  - For fødselsnummer skal status være en av: F-nr, D-nr, Fiktivt f-nr (vist som advarsel) eller Ugyldig (vist som feil). Feltet er påkrevd.
  - Klassekode: Feltet er påkrevd. Hvis som feil hvis tomt eller mangler.
  - Elevnavn: Feltet er ikke påkrevd. Hvis som advarsel hvis tomt eller mangler.

## Validering av F-nr og D-nr
Skriv Java kode for validering av f-nr, d-nr og fiktivt f-nr.

## Teknologier
- Java Virtual Threads
- Hexagonal architecture (skill mellom klasser for lagring og API)
- Dockerfile med distroless Java base image
- Dependabot med optimalisert/fornuftig grupperinger
- GitHub Actions for CI/CD
- Maven repo for egne artefakter: "https://repo.fintlabs.no/releases"
- Bruk de nyeste non-preview Java språkfunksjonalitet. Bruk automatisk linter for formatering.
- Enhetstester av all egen kode
- Ende-til-ende testing av API-kall mot Vigo Skole
- Beskriv APIet med OpenAPI
- Siste stabile release av:
  - Gradle
  - Spring Boot 4 og Spring Security
  - Java 26
  - JUnit 6
  - Eclipse Store for persistering
- Web app
  - NAV sitt designsystemet Aksel: https://aksel.nav.no
  - Spring Web MVC
  - Bruk ID-porten for identifisering av brukere.
  - CSRF protection