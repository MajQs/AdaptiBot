# Wytyczne Architektoniczne - Standardy Projektu

## 1. STRUKTURA MODUŁU (Package-by-Feature)

### 1.1 Podstawowa struktura pakietu
```
module/
├── ModuleFacade.kt              # PUBLICZNA - jedyny punkt wejścia
├── ModuleConfiguration.kt       # INTERNAL - konfiguracja Spring
├── *Properties.kt               # INTERNAL - properties na poziomie modułu
├── api/                         # Warstwa HTTP (Controllers, JSON mappers)
├── domain/                      # Logika biznesowa
│   ├── *Service.kt             # INTERNAL
│   ├── *Provider.kt            # INTERNAL
│   ├── *Factory.kt             # INTERNAL
│   ├── *Repository.kt          # INTERNAL - interfejsy portów
│   └── *Event.kt               # INTERNAL - eventy domenowe
├── dto/                         # PUBLICZNE - kontrakty wejścia/wyjścia fasady
│   ├── *InputDto.kt
│   └── *OutputDto.kt
└── adapter/                     # Implementacje portów (hexagonal)
    ├── hermes/                 # Event publishers
    ├── mongodb/                # Bazy danych
    ├── httpclient/             # Zewnętrzne HTTP API
    └── *Properties.kt          # Properties specyficzne dla adaptera
```

## 2. FASADA - JEDYNY PUNKT WEJŚCIA

### 2.1 Zasady
- **Jedna klasa Facade na moduł**
- **Widoczność: `class` (public) z `internal constructor`**
- **Przyjmuje i zwraca TYLKO obiekty z `dto/`**
- **Nie zawiera logiki biznesowej - deleguje do `domain/`**

### 2.2 Przykład
```kotlin
package pl.allegro.company.module

class ModuleFacade internal constructor(
    private val serviceOrProvider: ServiceOrProvider
) {
    fun performAction(input: InputDto): OutputDto =
        serviceOrProvider.execute(input)
}
```

## 3. KONFIGURACJA

### 3.1 Zasady
- **Jedna klasa Configuration na moduł**: `ModuleConfiguration.kt`
- **Widoczność: `internal class`**
- **Adnotacje: `@Configuration`, `@EnableConfigurationProperties`**
- **Zawiera metody `@Bean` dla Spring i metodę modułową dla testów**

### 3.2 Struktura
```kotlin
@EnableConfigurationProperties(ModuleProperties::class)
@Configuration
internal class ModuleConfiguration {

    @Bean
    fun moduleFacade(
        dependency: Dependency,
        repository: Repository,
        properties: ModuleProperties
    ): ModuleFacade = ModuleFacade(
        serviceOrProvider = ServiceOrProvider(dependency, repository, properties)
    )

    @Bean
    fun repository(/* spring deps */): Repository =
        RealRepository(/* deps */)

    // Metoda dla testów - zwraca moduł z InMemory implementacjami
    @JvmOverloads
    fun moduleModule(
        dependency: Dependency = InMemoryDependency(),
        repository: InMemoryRepository = InMemoryRepository()
    ): ModuleModule = ModuleModule(
        facade = moduleFacade(dependency, repository, ModuleProperties.default()),
        repository = repository
    )

    internal class ModuleModule(
        val facade: ModuleFacade,
        val repository: InMemoryRepository
    )
}
```

## 4. DTO - KONTRAKTY

### 4.1 Zasady
- **Lokalizacja: pakiet `dto/` w module**
- **Widoczność: PUBLICZNE (brak modyfikatora)**
- **Używane TYLKO jako input/output fasady**
- **Immutable data classes**
- **Nazewnictwo: `*Dto`, `*InputDto`, `*OutputDto`**

### 4.2 Przykład
```kotlin
package pl.allegro.company.module.dto

data class ModuleInputDto(
    val id: EntityId,
    val value: String,
    val marketplace: Marketplace
)

data class ModuleOutputDto(
    val result: ResultDto,
    val timestamp: Long
)
```

## 5. DOMAIN - LOGIKA BIZNESOWA

### 5.1 Zasady
- **Wszystkie klasy: `internal`**
- **Nie używają obiektów z `dto/` - konwersja na granicy**
- **Interfejsy portów (Repository, HttpClient): `internal interface`**

### 5.2 Nazewnictwo
- `*Service` - orchestracja logiki biznesowej
- `*Provider` - dostarczanie danych/obiektów
- `*Factory` - tworzenie obiektów domenowych
- `*Repository` - interfejs do persystencji
- `*Calculator` - kalkulacje/algorytmy
- `*Event` - eventy domenowe

### 5.3 Przykład
```kotlin
package pl.allegro.company.module.domain

internal class DataProvider(
    private val repository: DataRepository,
    private val calculator: Calculator
) {
    fun getData(input: DomainInput): DomainOutput =
        repository.find(input.id)
            .let { calculator.calculate(it) }
}

internal interface DataRepository {
    fun find(id: EntityId): Entity
}
```

## 6. ADAPTER - IMPLEMENTACJE PORTÓW

### 6.1 Zasady
- **Wszystkie klasy: `internal`**
- **Struktura: `adapter/{technology}/`**
- **Implementują interfejsy z `domain/`**
- **Dwie wersje: produkcyjna i InMemory (dla testów)**
- **Może posiadać własną klasę `*Configuration.kt`** (jeśli adapter wymaga złożonej konfiguracji)

### 6.2 Nazewnictwo
- Produkcyjna: `MongoDb*Repository`, `Hermes*Publisher`, `Remote*Client`
- Testowa: `InMemory*Repository`, `InMemory*Publisher`
- Persystencja: `Persistent*` (encje DB)

### 6.3 Przykład
```kotlin
// Interfejs w domain/
internal interface DataRepository {
    fun getData(): List<Data>
}

// Implementacja produkcyjna w adapter/mongodb/
@Repository
internal class MongoDbDataRepository(
    private val mongoTemplate: MongoTemplate,
    private val circuitBreaker: CircuitBreaker
) : DataRepository {
    @Cacheable("dataCache")
    override fun getData() =
        circuitBreaker.executeSupplier {
            mongoTemplate.findAll(PersistentData::class.java)
        }.map { it.toDomain() }

    // Mapowanie: Persistent -> Domain (private extension)
    private fun PersistentData.toDomain() = Data(
        id = DataId(id),
        value = value
    )
}

// Implementacja testowa w adapter/mongodb/
internal class InMemoryDataRepository(
    private val data: MutableList<Data> = mutableListOf()
) : DataRepository {
    override fun getData() = data.toList()

    fun save(vararg items: Data) = data.addAll(items)
}
```

### 6.4 Konfiguracja Adaptera (opcjonalna)
**Kiedy**: Adapter wymaga specyficznej konfiguracji (cache, converters, beans technologiczne)

```kotlin
// adapter/mongodb/MongoDbConfiguration.kt
@EnableConfigurationProperties(AdGroupCacheProperties::class)
@Configuration
internal class MongoDbConfiguration {

    @Bean
    fun adGroupRepository(
        mongoTemplate: MongoTemplate,
        circuitBreaker: CircuitBreaker,
        cache: org.springframework.cache.Cache
    ): AdGroupRepository =
        CachedAdGroupRepository(
            MongoDbAdGroupRepository(mongoTemplate, circuitBreaker),
            cache
        )

    @Bean
    fun adGroupCache(properties: AdGroupCacheProperties): Cache =
        // ... konfiguracja cache
}

// adapter/adsemissionconfig/PricingRulesRepositoryConfiguration.kt
@EnableConfigurationProperties(PricingRulesCacheProperties::class)
@Configuration
internal class PricingRulesRepositoryConfiguration {

    @Bean
    fun pricingRulesRepository(
        httpClient: AdsEmissionConfigHttpClient,
        cache: TwoLevelCache<Map<PricingRule, SurchargeMapping>>
    ): PricingRulesRepository =
        CachedPricingRulesRepository(
            RemotePricingRulesRepository(httpClient),
            cache
        )
}
```

## 7. SHARED KERNEL

### 7.1 Zasady
- **Lokalizacja: `sharedkernel/`**
- **Publiczne value objects używane przez wiele modułów**
- **Immutable data classes z walidacją**

### 7.2 Przykład
```kotlin
package pl.allegro.company.sharedkernel

data class EmissionId(val value: String) {
    companion object {
        @JvmStatic
        fun of(value: String) = EmissionId(value)
    }
}
```

## 8. PROPERTIES

### 8.1 Struktura
- **Poziom modułu**: w głównym pakiecie modułu (`*Properties.kt`)
- **Poziom adaptera**: w pakiecie adaptera
- **Adnotacja**: `@ConfigurationProperties(prefix = "...")`
- **Widoczność**: `internal data class`

### 8.2 Przykład
```kotlin
@ConfigurationProperties(prefix = "module.feature")
internal data class ModuleProperties(
    val enabled: Boolean,
    val timeout: Duration
) {
    companion object {
        fun default() = ModuleProperties(
            enabled = true,
            timeout = Duration.ofSeconds(5)
        )
    }
}
```

## 9. API LAYER (HTTP)

### 9.1 Zasady
- **Lokalizacja: pakiet `api/`**
- **Widoczność**: `internal` (nie są publicznym API modułu)
- **Tylko w module orkiestrującym (np. orchestrator)**
- **Controller deleguje do Facade**

### 9.2 Struktura
```kotlin
// api/EmissionController.kt
@RestController
@RequestMapping("/api/v1/emission")
internal class EmissionController(
    private val emissionHandler: EmissionHandler
) {
    @PostMapping
    fun getEmission(@RequestBody request: AdsRequestJson): EmissionJson =
        emissionHandler.handle(request)
}

// api/*Json.kt - obiekty HTTP (nie Dto!)
internal data class AdsRequestJson(
    val placement: String,
    val limit: Int
)
```

## 10. MAPOWANIA MIĘDZY WARSTWAMI

### 10.1 Zasady
**Mapowania odbywają się ZAWSZE na granicy warstw:**
- HTTP → DTO: w `api/` (Factory lub extension `toDto()`)
- DTO → Domain: w `domain/` (na wejściu)
- Domain → DTO: w `domain/` (na wyjściu)
- Domain → Persistent: w `adapter/` (private extension `toPersistent()`)
- Persistent → Domain: w `adapter/` (private extension `toDomain()`)

**Kierunek**: Zawsze mapuj NA obiekt wewnętrzny (Domain nie zna JSON, nie zna Persistent)

### 10.2 HTTP JSON → DTO (w api/)

#### Opcja A: Factory Pattern (preferowane dla złożonych mapowań)
```kotlin
// api/AdsRequestFactory.kt
internal class AdsRequestFactory(
    private val marketplaceMappingProperties: MarketplaceMappingProperties
) {
    fun createAdsRequestDto(requestWithHeaders: RequestWithHeaders): AdsRequestDto =
        requestWithHeaders.toAdsRequestDto()

    private fun RequestWithHeaders.toAdsRequestDto() = AdsRequestDto(
        cmuid = cmuid?.let { Cmuid(it) },
        userId = extractUserId(jwtToken),
        placement = Placement(placement),
        scoredOffers = request.offers.map {
            ScoredOffer(OfferId(it.id), it.score.toDouble())
        },
        marketplace = Marketplace(marketplaceMappingProperties.map(request.marketplace))
    )
}

// Użycie w Controller
@PostMapping
fun getAds(@RequestBody request: AdsRequestJson): AdsJson =
    adsRequestFactory.createAdsRequestDto(request)
        .let { emissionHandler.handle(it) }
```

#### Opcja B: Extension Function (dla prostych mapowań)
```kotlin
// api/events/AdGroupsJson.kt
@JsonIgnoreProperties(ignoreUnknown = true)
internal data class AdGroupsJson(
    @JsonProperty("adgroups") val adGroups: List<AdGroupJson>
) {
    fun toDto() = adGroups
        .filter(AdGroupJson::isSupported)
        .map(AdGroupJson::toDto)
}

internal data class AdGroupJson(
    @JsonProperty("id") val id: String,
    @JsonProperty("status") val status: String
) {
    fun toDto() = AdGroupDto(
        id = AdGroupId(id),
        activationStatus = ActivationStatus.valueOf(status)
    )
}

// Użycie w Controller
@PostMapping("/events/adgroups")
fun saveAdGroups(@RequestBody adGroupsJson: AdGroupsJson) =
    auctionServiceFacade.saveAdGroups(adGroupsJson.toDto())
```

### 10.3 Domain → DTO (w domain/)

```kotlin
// domain/RawBiddingOutput.kt
internal class RawBiddingOutput(
    private val offer: BiddingOfferInputDto,
    private val bid: Double,
    private val effectiveCpcLimit: Double
) {
    fun toDto(): OfferWithBidDto =
        OfferWithBidDto(offer.inventoryUnitId, bid, effectiveCpcLimit)
}

// domain/BidsProvider.kt
internal class BidsProvider(/* deps */) {
    fun calculateBids(input: BiddingInputDataDto): List<OfferWithBidDto> =
        calculateRawBids(input)
            .map { it.toDto() }  // Mapowanie na granicy
}
```

### 10.4 Domain ↔ Persistent (w adapter/)

```kotlin
// adapter/mongodb/PersistentAdGroup.kt
@Document(collection = "adgroups")
internal data class PersistentAdGroup(
    @Id val id: String,
    val s: String,      // status (skrócone nazwy dla MongoDB)
    val ts: Instant,    // timestamp
    val v: Long         // version
)

// adapter/mongodb/MongoDbAdGroupRepository.kt
internal class MongoDbAdGroupRepository(
    private val mongoTemplate: MongoTemplate
) : AdGroupRepository {

    override fun save(adGroups: List<AdGroupDto>) {
        adGroups
            .map { it.toPersistent() }
            .forEach { mongoTemplate.save(it) }
    }

    override fun findById(id: AdGroupId): AdGroupDto? =
        mongoTemplate.findById(id.value, PersistentAdGroup::class.java)
            ?.toDomain()

    // Mapowania jako private extension functions
    private fun AdGroupDto.toPersistent() = PersistentAdGroup(
        id = id.value,
        s = activationStatus.toString(),
        ts = dateTimeProvider.currentTime().toInstant(),
        v = version
    )

    private fun PersistentAdGroup.toDomain() = AdGroupDto(
        id = AdGroupId(id),
        activationStatus = ActivationStatus.valueOf(s),
        version = v
    )
}
```

### 10.5 Konwencje mapowań

| Warstwa Źródłowa | Warstwa Docelowa | Gdzie | Jak | Nazwa metody |
|------------------|------------------|-------|-----|--------------|
| HTTP JSON | DTO | `api/` | Factory lub Extension | `toDto()` lub `create*Dto()` |
| DTO | Domain | `domain/` | Constructor lub Factory | Direct construction |
| Domain | DTO | `domain/` | Extension | `toDto()` |
| Domain | Persistent | `adapter/` | Private Extension | `toPersistent()` |
| Persistent | Domain | `adapter/` | Private Extension | `toDomain()` |
| Domain | Event | `domain/` | Extension/Factory | `toEvent()` |

### 10.6 Zasady mapowań
- ✅ Extension function `toDto()` / `toDomain()` / `toPersistent()`
- ✅ Private w adapterach (nie eksponuj poza adapter)
- ✅ Internal w api/ (mapowania JSON → DTO)
- ✅ Factory dla złożonych mapowań z dodatkową logiką
- ✅ Walidacja w konstruktorze Value Object (nie w mapowaniu)
- ❌ Nigdy nie przekazuj JSON do domain
- ❌ Nigdy nie przekazuj Persistent poza adapter
- ❌ Nie twórz publicznych funkcji mapujących (tylko extension na granicach)

## 11. TESTY

## 11. TESTY

### 11.1 Testy Integracyjne (E2E)
- **Lokalizacja**: `src/integration/groovy/`
- **Framework**: Spock (`*Spec.groovy`)
- **Bazowa klasa**: `BaseIntegrationSpec`
- **Profil**: `@ActiveProfiles("integration")`
- **Testują**: cały flow przez HTTP API

### 11.2 Testy Jednostkowe
- **Lokalizacja**: `src/test/kotlin/`
- **Framework**: JUnit/Kotest
- **Testują**: TYLKO fasady i logikę domenową
- **Używają**: InMemory implementacji z `*Module`

### 11.3 Moduł testowy (z Configuration)
```kotlin
// Użycie w teście
val module = BiddingConfiguration().biddingModule(
    biddingPlacementGroupProperties = BiddingPlacementGroupProperties.default(),
    emissionBiddingPublisher = InMemoryEmissionBiddingEventPublisher(),
    autobiddingTestAdGroupRepository = InMemoryAutobiddingTestAdGroupRepository()
)

// Test
module.facade.calculateBids(input)
module.autobiddingTestAdGroupRepository.save(testData)
```

## 12. KONWENCJE NAZEWNICZE

### 12.1 Klasy
| Typ | Konwencja | Przykład |
|-----|-----------|----------|
| Fasada | `*Facade` | `BiddingFacade` |
| Konfiguracja | `*Configuration` | `BiddingConfiguration` |
| DTO | `*Dto`, `*InputDto`, `*OutputDto` | `BiddingInputDataDto` |
| Service | `*Service` | `CategoryService` |
| Provider | `*Provider` | `BidsProvider`, `EmissionProvider` |
| Repository | `*Repository` | `AutobiddingTestAdGroupRepository` |
| Factory | `*Factory` | `EmissionFactory` |
| Calculator | `*Calculator` | `VanillaBidCalculator` |
| Event | `*Event` | `EmissionEvent` |
| Properties | `*Properties` | `BiddingPlacementGroupProperties` |
| Handler | `*Handler` | `EmissionHandler` |
| Adapter (prod) | `{Tech}*` | `MongoDbDataRepository` |
| Adapter (test) | `InMemory*` | `InMemoryDataRepository` |
| Encja DB | `Persistent*` | `PersistentAdGroup` |

### 12.2 Zmienne (wstrzykiwanie)
- **Fasady**: pełna nazwa z sufixem `Facade`
  ```kotlin
  private val biddingFacade: BiddingFacade
  private val cappingFacade: CappingFacade
  ```
- **Inne zależności**: camelCase odpowiadający nazwie typu
  ```kotlin
  private val bidsProvider: BidsProvider
  private val emissionFactory: EmissionFactory
  private val mongoTemplate: MongoTemplate
  ```

## 13. WIDOCZNOŚĆ (VISIBILITY)

| Element | Widoczność | Uzasadnienie |
|---------|------------|--------------|
| Facade (klasa) | `class` (public) | Jedyny punkt wejścia modułu |
| Facade (constructor) | `internal constructor` | Tylko Configuration tworzy |
| Configuration | `internal class` | Tylko Spring skanuje |
| DTO | `data class` (public) | Kontrakt publiczny modułu |
| Domain (wszystko) | `internal` | Szczegóły implementacji |
| Adapter (wszystko) | `internal` | Szczegóły implementacji |
| Properties | `internal` | Konfiguracja wewnętrzna |
| API (Controllers, JSON) | `internal` | Nie są API modułu |

## 14. DEPENDENCY FLOW

```
HTTP Request → Controller (api/) → Facade (publiczny)
                                      ↓
                                   Service/Provider (domain/)
                                      ↓
                                   Repository (domain/ - interface)
                                      ↓
                                   Adapter (adapter/ - implementation)
```

**Zasada**: Zależności wskazują do wewnątrz. Domain nie zna Adapter. Adapter implementuje interfejsy z Domain.

## 15. CHECKLIST NOWEGO MODUŁU

- [ ] Jeden plik `ModuleFacade.kt` z `internal constructor`
- [ ] Jeden plik `ModuleConfiguration.kt` z `@Configuration`
- [ ] Pakiet `dto/` z publicznymi DTO (input/output fasady)
- [ ] Pakiet `domain/` z `internal` klasami biznesowymi
- [ ] Interfejsy portów (`*Repository`, `*Client`) w `domain/`
- [ ] Pakiet `adapter/` z implementacjami portów
- [ ] Implementacje `InMemory*` dla testów
- [ ] Metoda `*Module()` w Configuration dla testów jednostkowych
- [ ] Properties z `companion object fun default()`
- [ ] Wszystkie nazwy zgodne z konwencją
- [ ] Zmienne wstrzykiwane nazwane jak typ (camelCase)
- [ ] Fasady wstrzykiwane z sufixem `Facade`
- [ ] Value objects w `sharedkernel/` jeśli współdzielone
- [ ] Adapter ma strukturę `adapter/{technology}/`
- [ ] Mapowania na granicach warstw (toDto/toDomain/toPersistent)
- [ ] Mapowania jako private extension w adapterach

## 16. ANTY-WZORCE (CZEGO UNIKAĆ)

❌ Publiczne klasy w `domain/` lub `adapter/`
❌ DTO używane wewnątrz domain (mapuj na granicy)
❌ Logika biznesowa w Facade (tylko delegacja)
❌ Więcej niż jedna Facade w module
❌ Configuration jako `class` (public)
❌ Adaptery bez InMemory wersji do testów
❌ Properties bez `default()` w companion object
❌ Repository z implementacją w `domain/`
❌ Bezpośrednie wywołania HTTP/DB z domain
❌ Mieszanie API JSON objects z DTO
❌ Przekazywanie JSON do domain
❌ Przekazywanie Persistent poza adapter
❌ Publiczne funkcje mapujące (tylko private extension)
❌ Mapowania w złym miejscu (np. JSON→Domain bez DTO)

