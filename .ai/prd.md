# Dokument wymagań produktu (PRD) - AdaptiBot

## Historia zmian

| Wersja | Data | Zmiany |
|--------|------|--------|
| v1.2 | 2025-11-21 | - Usunięto sekcję 3.7.3 (Eksport logów)<br>- Usunięto RF-090, RF-091, RF-092 (eksport logów)<br>- Usunięto RF-110 (undo/redo)<br>- Dodano wsparcie dla dystrybucji .jar (RF-129)<br>- Dodano mechanizm grupowania akcji (RF-130 do RF-133)<br>- Usunięto US-032 (filtrowanie logów)<br>- Usunięto US-038 (komentarze do kroków)<br>- US-043: Skróty klawiszowe są teraz stałe w aplikacji<br>- US-044: Ustawienia zapisywane razem ze skryptem<br>- Usunięto US-048 (anulowanie długich operacji)<br>- US-051: Limit historii logów zmieniony na 1000 wpisów<br>- Dodano US-059: Grupowanie akcji w bloki<br>- 6.2.1: Opóźnienie reakcji Obserwatora konfigurowalne (domyślnie 1s)<br>- Usunięto sekcję 6.5 (Metryki adopcji) |
| v1.1 | 2025-11-20 | - Usunięto RF-058 (wizualizacja aktywnego Obserwatora)<br>- Zmieniono mechanizm działania Obserwatorów: przejście z synchronicznego sprawdzania przed każdym krokiem na asynchroniczne sprawdzanie w osobnym wątku<br>- Dodano nową sekcję 3.5.5: Synchronizacja wątków Obserwatora (RF-064 do RF-067)<br>- Dodano RF-073: System uruchamia Obserwatorów w osobnym wątku<br>- Zaktualizowano metryki wydajności Obserwatorów<br>- Zaktualizowano założenia architektoniczne o wielowątkowość |
| v1.0 | 2025-11-20 | Wersja początkowa dokumentu |

## 1. Przegląd produktu

### 1.1 Nazwa produktu
AdaptiBot

### 1.2 Opis produktu
AdaptiBot to zaawansowana aplikacja desktopowa przeznaczona dla systemu Windows, umożliwiająca automatyzację złożonych interakcji użytkownika z interfejsem systemu operacyjnego. Aplikacja pozwala na definiowanie, zapisywanie i wykonywanie skryptów automatyzacyjnych, które emulują działania myszy, klawiatury oraz inne interakcje z elementami interfejsu graficznego.

### 1.3 Wersja MVP
Niniejszy dokument opisuje wymagania dla wersji Minimum Viable Product (MVP), której głównym celem jest dostarczenie funkcjonalnego narzędzia automatyzacji z priorytetem na prostotę interfejsu użytkownika i stabilność działania.

### 1.4 Platforma docelowa
- System operacyjny: Windows 10/11 (dystrybucja .exe), Linux/macOS (dystrybucja .jar)
- Architektura: x64
- Dystrybucja: Samodzielny plik wykonywalny (.exe) dla Windows oraz plik .jar dla innych systemów

### 1.5 Stos technologiczny
- Język programowania: Kotlin
- Środowisko uruchomieniowe: JVM (Java 21+ LTS)
- Framework UI: JavaFX
- Integracja z systemem: JNA/JNI dla WinAPI
- Przetwarzanie obrazu: OpenCV (lub równoważna biblioteka z bindingami Kotlin/Java)
- Format zapisu: JSON z osadzonymi obrazami w Base64

### 1.6 Główne założenia architektoniczne
- Architektura modułowa z separacją warstw (Core Executor, UI Layer, Automation Layer, Vision Layer)
- Model danych oparty na strukturze drzewiastej (Composite Pattern) dla nieograniczonej liczby kroków i zagnieżdżeń
- State Machine/Execution Manager dla zarządzania przepływem wykonania
- Wielowątkowa architektura wykonania: osobny wątek dla Obserwatorów, główny wątek dla skryptu
- Warstwa abstrakcji dla operacji automatyzacji (IElementFinder, IActionExecutor)
- Architektura przygotowana pod przyszłą migrację do edytora wizualnego (node-based)

## 2. Problem użytkownika

### 2.1 Grupa docelowa
Użytkownicy zaawansowani, administratorzy systemów, testerzy oprogramowania oraz specjaliści automatyzacji procesów biznesowych, którzy:
- Potrzebują automatyzować powtarzalne zadania w środowisku Windows
- Wymagają niezawodnego mechanizmu obsługi nieprzewidzianych zdarzeń podczas automatyzacji
- Potrzebują definiować złożone scenariusze z logiką warunkową
- Muszą uruchamiać procesy automatyzacji bez nadzoru przez długi czas

### 2.2 Problemy do rozwiązania
1. Brak prostych narzędzi do tworzenia złożonych scenariuszy automatyzacji z obsługą błędów i zdarzeń
2. Trudność w obsłudze nieprzewidzianych elementów interfejsu (np. wyskakujące okna, reklamy) podczas wykonywania skryptów
3. Potrzeba ciągłego nadzoru nad działającymi skryptami automatyzacji
4. Brak elastycznych mechanizmów warunkowych w istniejących rozwiązaniach
5. Trudność w identyfikacji elementów UI, które mogą zmieniać pozycję lub wygląd
6. Skomplikowane narzędzia wymagające zaawansowanej wiedzy programistycznej

### 2.3 Wartość dodana
AdaptiBot rozwiązuje powyższe problemy poprzez:
- Prosty, intuicyjny interfejs edytora listowego/tabelarycznego
- Zaawansowany mechanizm Obserwatorów (Listeners) działających asynchronicznie w osobnym wątku, reagujących na zdarzenia w czasie rzeczywistym bez spowalniania głównego skryptu
- Niezawodne wykonywanie skryptów z automatycznym logowaniem i obsługą błędów
- Elastyczną identyfikację elementów (koordynaty + rozpoznawanie obrazu)
- Pełną obsługę logiki warunkowej (IF/ELSE, AND/OR/NOT)
- Możliwość działania w nieskończonej pętli z mechanizmem bezpiecznego zatrzymania

## 3. Wymagania funkcjonalne

### 3.1 Zarządzanie skryptami

#### 3.1.1 Tworzenie skryptów
- RF-001: System umożliwia tworzenie nowego, pustego skryptu
- RF-002: System pozwala na dodawanie kroków do skryptu bez ograniczeń liczby
- RF-003: System wspiera nieograniczoną głębokość zagnieżdżenia bloków (warunki, pętle, obserwatorzy)
- RF-004: Każdy krok skryptu posiada unikalny identyfikator w ramach skryptu

#### 3.1.2 Edycja skryptów
- RF-005: System umożliwia edycję istniejących kroków skryptu
- RF-006: System pozwala na usuwanie kroków z możliwością cofnięcia operacji
- RF-007: System umożliwia zmianę kolejności kroków (przesuwanie w górę/dół)
- RF-008: System pozwala na kopiowanie i wklejanie pojedynczych kroków oraz całych bloków
- RF-009: System wspiera zagnieżdżanie kroków w blokach strukturalnych (IF/ELSE, Obserwator)

#### 3.1.3 Zapis i wczytywanie skryptów
- RF-010: System zapisuje skrypty w formacie JSON
- RF-011: Obrazy używane do rozpoznawania wzorca są osadzane w pliku JSON jako Base64
- RF-012: System waliduje poprawność struktury skryptu przy wczytywaniu
- RF-013: System informuje użytkownika o błędach w strukturze wczytanego skryptu
- RF-014: System umożliwia eksport skryptu do pojedynczego pliku
- RF-015: System umożliwia import skryptu z pliku

### 3.2 Typy akcji i kroków

#### 3.2.1 Akcje myszy
- RF-016: System emuluje kliknięcie lewym przyciskiem myszy na określonych koordynatach X/Y
- RF-017: System emuluje kliknięcie prawym przyciskiem myszy na określonych koordynatach X/Y
- RF-018: System emuluje podwójne kliknięcie na określonych koordynatach X/Y
- RF-019: System emuluje przesunięcie kursora myszy do określonych koordynat X/Y
- RF-020: System emuluje przeciąganie myszy (drag and drop) między dwoma punktami
- RF-021: System emuluje scrollowanie myszy (w górę/dół, lewo/prawo)

#### 3.2.2 Akcje klawiatury
- RF-022: System emuluje wpisanie tekstu z klawiatury
- RF-023: System emuluje naciśnięcie pojedynczego klawisza
- RF-024: System emuluje kombinacje klawiszy (np. Ctrl+C, Alt+Tab)
- RF-025: System emuluje naciśnięcie klawiszy specjalnych (Enter, Escape, Tab, etc.)

#### 3.2.3 Akcje systemowe
- RF-026: System umożliwia dodanie opóźnienia (czekania) o określonym czasie w milisekundach
- RF-027: System umożliwia uruchomienie zewnętrznej aplikacji lub polecenia systemowego
- RF-028: System umożliwia zamknięcie określonej aplikacji lub okna

#### 3.2.4 Akcje przepływu sterowania
- RF-029: System umożliwia przerwanie wykonywania skryptu (akcja Stop)
- RF-030: System umożliwia przejście do określonego kroku (akcja Skocz do akcji)
- RF-031: System umożliwia kontynuację od następnego kroku (akcja Kontynuuj)

#### 3.2.5 Grupowanie akcji
- RF-130: System umożliwia utworzenie bloku grupującego akcje
- RF-131: Blok grupujący może zawierać dowolną liczbę kroków i innych bloków
- RF-132: Blok grupujący posiada nazwę (np. "Logowanie", "Wyszukiwanie produktu")
- RF-133: Bloki grupujące można rozwijać i zwijać w edytorze dla lepszej czytelności

### 3.3 Identyfikacja elementów

#### 3.3.1 Identyfikacja przez koordynaty
- RF-032: System umożliwia określenie elementu przez współrzędne X/Y
- RF-033: System pozwala na bezpośrednie wpisanie wartości X/Y
- RF-034: System umożliwia przechwycenie koordynat poprzez kliknięcie na ekranie (narzędzie do przechwytywania)

#### 3.3.2 Identyfikacja przez rozpoznawanie obrazu
- RF-035: System umożliwia przechwycenie fragmentu ekranu jako wzorca do rozpoznawania
- RF-036: System pozwala na wczytanie obrazu wzorca z pliku
- RF-037: System przeszukuje ekran w celu znalezienia najlepszego dopasowania do wzorca
- RF-038: System umożliwia ustawienie progu dopasowania obrazu (np. 70%)
- RF-039: System wizualizuje przechwycony obszar wzorca w interfejsie użytkownika
- RF-040: System zwraca współrzędne znalezionego elementu (środek znalezionego obszaru)
- RF-041: System loguje informację, gdy element nie został znaleziony z wystarczającym progiem dopasowania

### 3.4 Logika warunkowa

#### 3.4.1 Bloki IF/ELSE
- RF-042: System umożliwia utworzenie bloku warunkowego IF
- RF-043: System umożliwia dodanie alternatywnej gałęzi ELSE
- RF-044: System umożliwia zagnieżdżanie bloków IF/ELSE bez ograniczeń głębokości

#### 3.4.2 Warunki sprawdzania
- RF-045: System umożliwia sprawdzenie czy element (obraz) istnieje na ekranie
- RF-046: System umożliwia sprawdzenie czy element NIE istnieje na ekranie
- RF-047: System umożliwia złożenie wielu warunków operatorem AND
- RF-048: System umożliwia złożenie wielu warunków operatorem OR
- RF-049: System umożliwia negację warunku operatorem NOT
- RF-050: System pozwala na tworzenie złożonych wyrażeń logicznych z wieloma operatorami i nawiasami

### 3.5 Mechanizm Obserwatora (Listener)

#### 3.5.1 Definicja Obserwatora
- RF-051: System umożliwia utworzenie Obserwatora w dowolnym bloku skryptu
- RF-052: Obserwator zawiera warunek (sprawdzający obecność elementu) oraz sekwencję akcji do wykonania
- RF-053: System pozwala na definiowanie wielu Obserwatorów w jednym skrypcie
- RF-054: Obserwator może zawierać dowolne akcje, w tym akcje przepływu sterowania

#### 3.5.2 Działanie Obserwatora
- RF-055: System sprawdza warunki wszystkich aktywnych Obserwatorów w osobnym, dedykowanym wątku równolegle do wykonywania głównego skryptu
- RF-056: Gdy warunek Obserwatora zostanie spełniony, system wstrzymuje główny przepływ i wykonuje sekwencję akcji Obserwatora
- RF-057: Po zakończeniu sekwencji Obserwatora, system wraca do przerwane akcji głównego skryptu (chyba że Obserwator wykonał akcję Skocz do akcji)

#### 3.5.3 Priorytet Obserwatorów
- RF-058: Obserwatorzy zagnieżdżone głębiej mają wyższy priorytet niż obserwatorzy na wyższych poziomach
- RF-059: Na tym samym poziomie zagnieżdżenia, priorytet ma Obserwator zdefiniowany wcześniej (pierwszy w kolejności)
- RF-060: System wykonuje tylko jednego Obserwatora na raz (pierwszy spełniony warunek według priorytetu)

#### 3.5.4 Zasięg Obserwatora
- RF-061: Obserwator jest aktywny tylko w kontekście bloku, w którym został zdefiniowany
- RF-062: Obserwator zdefiniowany w bloku IF jest aktywny tylko podczas wykonywania tego bloku IF
- RF-063: Obserwator zdefiniowany na poziomie głównym skryptu jest aktywny przez cały czas wykonywania skryptu

#### 3.5.5 Synchronizacja wątków Obserwatora
- RF-064: System zapewnia bezpieczną synchronizację między wątkiem Obserwatora a głównym wątkiem skryptu
- RF-065: Gdy Obserwator wykryje spełniony warunek, wątek główny skryptu jest wstrzymywany w bezpieczny sposób
- RF-066: Po wykonaniu akcji Obserwatora, wątek główny skryptu jest wznawiany od przerwane akcji
- RF-067: System zapewnia atomowość operacji przełączania kontekstu między wątkami

### 3.6 Wykonywanie skryptów

#### 3.6.1 Sterowanie wykonaniem
- RF-068: System umożliwia uruchomienie skryptu
- RF-069: System umożliwia zatrzymanie wykonywania skryptu w dowolnym momencie
- RF-070: System umożliwia wstrzymanie (pauza) i wznowienie wykonywania skryptu
- RF-071: Skrypt wykonuje się w nieskończonej pętli do momentu manualnego zatrzymania przez użytkownika
- RF-072: System wizualizuje aktualnie wykonywany krok w interfejsie użytkownika
- RF-073: System uruchamia Obserwatorów w osobnym wątku niezależnym od głównego wątku wykonywania skryptu

#### 3.6.2 Obsługa błędów
- RF-074: System loguje wyjątki występujące podczas wykonywania kroków
- RF-075: System kontynuuje wykonywanie kolejnego kroku po wystąpieniu błędu (nie zatrzymuje skryptu)
- RF-076: System zapisuje szczegółowe informacje o błędzie (typ wyjątku, komunikat, timestamp, krok)
- RF-077: System wizualizuje informację o ostatnio wystąpionym błędzie w interfejsie

#### 3.6.3 Synchronizacja i opóźnienia
- RF-078: System umożliwia definiowanie czasu oczekiwania przed każdą akcją
- RF-079: System umożliwia definiowanie czasu oczekiwania po każdej akcji
- RF-080: System optymalizuje sprawdzanie warunków Obserwatorów w osobnym wątku, aby nie blokować wykonywania głównego skryptu

### 3.7 Logowanie i debugowanie

#### 3.7.1 Historia wykonania
- RF-081: System rejestruje historię wszystkich wykonanych akcji z timestampem
- RF-082: Historia jest dostępna w interfejsie użytkownika w czasie rzeczywistym
- RF-083: Historia jest nietrwała (czyści się po zamknięciu aplikacji lub restarcie skryptu)
- RF-084: Każdy wpis w historii zawiera: timestamp, nazwę kroku, status wykonania, czas trwania

#### 3.7.2 Tryb debugowania
- RF-085: System umożliwia włączenie trybu debugowania
- RF-086: W trybie debugowania system wykonuje skrypt krok po kroku (wymaga potwierdzenia od użytkownika)
- RF-087: W trybie debugowania system wyświetla szczegółowe informacje o każdym kroku przed jego wykonaniem
- RF-088: System umożliwia ustawienie punktów przerwania (breakpoints) na wybranych krokach
- RF-089: System zatrzymuje wykonywanie na punktach przerwania i czeka na akcję użytkownika


### 3.8 Interfejs użytkownika

#### 3.8.1 Edytor skryptów (Opcja A - MVP)
- RF-093: Interfejs prezentuje skrypt w formie edytowalnej listy/tabeli kroków
- RF-094: Każdy krok wyświetla podstawowe informacje: typ akcji, parametry, identyfikator
- RF-095: Interfejs wizualizuje zagnieżdżenia poprzez wcięcia lub grupowanie
- RF-096: Interfejs umożliwia rozwijanie i zwijanie bloków strukturalnych (IF/ELSE, Obserwatorzy)
- RF-097: Interfejs udostępnia przyciski do dodawania, usuwania, edycji i przesuwania kroków
- RF-098: Interfejs udostępnia formularz edycji parametrów wybranego kroku

#### 3.8.2 Panel wykonywania
- RF-099: Interfejs zawiera panel sterowania z przyciskami: Start, Stop, Pauza, Wznów
- RF-100: Interfejs wyświetla status wykonywania: zatrzymany, działający, wstrzymany
- RF-101: Interfejs wyświetla aktualnie wykonywany krok (podświetlenie w liście)
- RF-102: Interfejs wyświetla licznik wykonanych iteracji pętli głównej

#### 3.8.3 Panel logów i debugowania
- RF-103: Interfejs zawiera panel z historią wykonanych akcji w czasie rzeczywistym
- RF-105: Panel logów umożliwia czyszczenie historii
- RF-106: Interfejs zawiera przełącznik trybu debugowania
- RF-107: W trybie debugowania interfejs wyświetla przycisk "Następny krok"

#### 3.8.4 Ogólne wymagania UI
- RF-108: Interfejs jest responsywny i płynny w obsłudze
- RF-109: Interfejs używa intuicyjnych ikon i etykiet
- RF-111: Interfejs wyświetla komunikaty o błędach i ostrzeżenia w zrozumiały sposób
- RF-112: Interfejs wspiera skróty klawiszowe dla najczęstszych operacji

### 3.9 Narzędzia pomocnicze

#### 3.9.1 Narzędzie przechwytywania koordynat
- RF-113: System udostępnia narzędzie do przechwytywania pozycji kursora myszy na ekranie
- RF-114: Narzędzie wyświetla aktualne koordynaty X/Y w czasie rzeczywistym
- RF-115: Narzędzie pozwala na zatwierdzenie wybranej pozycji jednym kliknięciem
- RF-116: Zatwierdzone koordynaty są automatycznie wstawiane do edytowanego kroku

#### 3.9.2 Narzędzie przechwytywania obrazu
- RF-117: System udostępnia narzędzie do zaznaczania obszaru ekranu jako wzorca
- RF-118: Narzędzie umożliwia swobodne zaznaczenie prostokątnego obszaru
- RF-119: Narzędzie wyświetla podgląd przechwyconego obrazu przed zatwierdzeniem
- RF-120: Przechwycony obraz jest automatycznie dołączany do edytowanego kroku

### 3.10 Dystrybucja i instalacja

#### 3.10.1 Pakowanie aplikacji
- RF-121: Aplikacja jest pakowana jako samodzielny plik wykonywalny .exe dla Windows
- RF-122: Plik wykonywalny .exe zawiera wszystkie niezbędne zależności (JVM, biblioteki)
- RF-123: Aplikacja .exe nie wymaga instalacji Java na komputerze docelowym
- RF-124: Proces budowania używa jpackage do tworzenia pliku wykonywalnego
- RF-129: Aplikacja jest również dostępna jako plik .jar do uruchamiania na innych systemach operacyjnych (np. Linux, macOS)

#### 3.10.2 Wymagania systemowe
- RF-125: Aplikacja uruchamia się na Windows 10 i nowszych (dla .exe) oraz na Linux/macOS z zainstalowaną Java 21+ (dla .jar)
- RF-126: Aplikacja wspiera architekturę x64
- RF-127: Aplikacja wymaga minimalnie 512 MB RAM
- RF-128: Aplikacja wymaga minimalnie 200 MB przestrzeni dyskowej

## 4. Granice produktu

### 4.1 Funkcje zawarte w MVP

W zakresie MVP znajdują się:
- Podstawowy edytor skryptów w formie listy/tabeli (Opcja A)
- Wszystkie typy akcji myszy i klawiatury wymienione w sekcji 3.2
- Obie strategie identyfikacji elementów (koordynaty i rozpoznawanie obrazu)
- Pełna obsługa logiki warunkowej (IF/ELSE, AND/OR/NOT)
- Kompletny mechanizm Obserwatorów z priorytetami i zasięgiem
- Wykonywanie w nieskończonej pętli z obsługą błędów
- System logowania i podstawowy tryb debugowania
- Dystrybucja jako samodzielny plik .exe

### 4.2 Funkcje poza zakresem MVP

Poza zakresem MVP znajdują się:
- Wizualny edytor typu node-based (Opcja B) - architektura przygotowana, implementacja w przyszłych wersjach
- Zaawansowane mechanizmy synchronizacji między wieloma instancjami skryptów
- Zdalne sterowanie skryptami przez sieć
- Integracja z zewnętrznymi systemami (bazy danych, API)
- Mechanizmy uwierzytelniania i autoryzacji użytkowników (MVP to aplikacja lokalna, jednoosobowa)
- Zapisywanie skryptów w chmurze
- Marketplace lub biblioteka gotowych skryptów
- Makrorejestracja (nagrywanie akcji użytkownika do skryptu) - może być dodane w przyszłych wersjach
- Obsługa wielu monitorów (MVP zakłada jeden ekran)
- Zaawansowana analityka wykonania skryptów (wykresy, raporty)
- Współdzielenie skryptów między użytkownikami
- System powiadomień (email, SMS) o statusie wykonania

### 4.3 Ograniczenia techniczne

- Aplikacja jest optymalizowana dla Windows (10/11), wsparcie dla Linux/macOS jako .jar (może wymagać dodatkowej konfiguracji dla automatyzacji)
- Rozpoznawanie obrazu jest ograniczone do dopasowania szablonu (template matching) - brak zaawansowanego AI/ML
- Aplikacja wymaga uprawnień systemowych do emulacji zdarzeń myszy i klawiatury
- Wydajność rozpoznawania obrazu zależy od rozdzielczości ekranu i rozmiaru wzorca
- Brak wsparcia dla akcji wymagających specjalnych uprawnień administratora (bez dodatkowej konfiguracji)

### 4.4 Założenia

- Użytkownik posiada podstawową wiedzę o automatyzacji i strukturach programistycznych (warunki, pętle)
- Użytkownik ma dostęp do systemu Windows z uprawnieniami standardowego użytkownika (lub Linux/macOS z Java 21+ dla wersji .jar)
- Aplikacje/interfejsy, które mają być automatyzowane, są dostępne i widoczne na ekranie
- Użytkownik manualnie monitoruje wykonywanie krytycznych skryptów (MVP to narzędzie wspomagające, nie bezobsługowe)

### 4.5 Przyszły rozwój

Architektura MVP jest przygotowana pod rozwój w następujących kierunkach:
- Migracja do wizualnego edytora node-based (Opcja B)
- Dodanie mechanizmu makrorejestracji
- Rozszerzenie o zaawansowane metody identyfikacji elementów (OCR, UI Automation API)
- Implementacja systemu wtyczek (plugins) dla custom akcji
- Wsparcie dla skryptów wielowątkowych

## 5. Historyjki użytkowników

### 5.1 Zarządzanie skryptami

US-001: Tworzenie nowego skryptu
Jako użytkownik chcę utworzyć nowy, pusty skrypt, aby móc zdefiniować sekwencję automatyzacji od początku.

Kryteria akceptacji:
- Aplikacja umożliwia utworzenie nowego skryptu poprzez menu lub przycisk "Nowy skrypt"
- Nowy skrypt jest pusty i gotowy do edycji
- Aplikacja pyta o zapisanie zmian w aktualnie edytowanym skrypcie przed utworzeniem nowego
- Nowy skrypt otrzymuje domyślną nazwę (np. "Nowy skrypt 1")

US-002: Zapisywanie skryptu do pliku
Jako użytkownik chcę zapisać stworzony skrypt do pliku JSON, aby móc go później wczytać i użyć ponownie.

Kryteria akceptacji:
- Aplikacja umożliwia zapisanie skryptu poprzez menu "Zapisz" lub "Zapisz jako"
- Skrypt jest zapisywany w formacie JSON
- Obrazy użyte do rozpoznawania są osadzone w pliku jako Base64
- Użytkownik może wybrać lokalizację i nazwę pliku
- Aplikacja informuje o sukcesie lub błędzie zapisu

US-003: Wczytywanie skryptu z pliku
Jako użytkownik chcę wczytać zapisany wcześniej skrypt z pliku JSON, aby kontynuować pracę lub uruchomić go ponownie.

Kryteria akceptacji:
- Aplikacja umożliwia wczytanie skryptu poprzez menu "Otwórz"
- Aplikacja waliduje poprawność struktury JSON
- Obrazy w formacie Base64 są poprawnie dekodowane
- W przypadku błędów walidacji użytkownik otrzymuje szczegółowy komunikat
- Aplikacja pyta o zapisanie zmian w aktualnie edytowanym skrypcie przed wczytaniem nowego

US-004: Edycja nazwy i opisu skryptu
Jako użytkownik chcę móc nadać skryptowi nazwę i opis, aby łatwiej identyfikować różne skrypty.

Kryteria akceptacji:
- Aplikacja pozwala na edycję nazwy skryptu
- Aplikacja pozwala na edycję opcjonalnego opisu skryptu
- Nazwa skryptu jest wyświetlana w tytule okna aplikacji
- Nazwa i opis są zapisywane w pliku JSON

### 5.2 Dodawanie i edycja kroków

US-005: Dodawanie kroku akcji myszy
Jako użytkownik chcę dodać krok kliknięcia myszy w określonym miejscu, aby zautomatyzować interakcję z przyciskiem lub elementem interfejsu.

Kryteria akceptacji:
- Aplikacja umożliwia dodanie nowego kroku z menu kontekstowego lub przycisku
- Dostępne typy akcji myszy: lewe kliknięcie, prawe kliknięcie, podwójne kliknięcie, przesunięcie, przeciąganie, scrollowanie
- Użytkownik może określić pozycję poprzez wpisanie koordynat X/Y lub użycie narzędzia przechwytywania
- Krok jest dodawany do aktualnie wybranej pozycji w skrypcie
- Krok jest natychmiast widoczny w edytorze

US-006: Dodawanie kroku akcji klawiatury
Jako użytkownik chcę dodać krok wpisania tekstu lub naciśnięcia klawiszy, aby zautomatyzować wprowadzanie danych.

Kryteria akceptacji:
- Aplikacja umożliwia dodanie kroku wpisania tekstu
- Aplikacja umożliwia dodanie kroku naciśnięcia pojedynczego klawisza
- Aplikacja umożliwia dodanie kroku kombinacji klawiszy (np. Ctrl+C)
- Dostępna lista klawiszy specjalnych (Enter, Escape, Tab, strzałki, itp.)
- Użytkownik może wprowadzić tekst do wpisania w formularzu edycji

US-007: Usuwanie kroku
Jako użytkownik chcę usunąć niepotrzebny krok ze skryptu, aby uprościć lub skorygować sekwencję automatyzacji.

Kryteria akceptacji:
- Aplikacja umożliwia usunięcie wybranego kroku poprzez przycisk lub klawisz Delete
- Aplikacja prosi o potwierdzenie usunięcia
- Usunięcie kroku jest możliwe do cofnięcia (undo)
- Po usunięciu bloku strukturalnego (IF, Obserwator) wszystkie zawarte w nim kroki są również usuwane

US-008: Edycja parametrów kroku
Jako użytkownik chcę zmienić parametry istniejącego kroku, aby skorygować jego działanie bez konieczności usuwania i dodawania na nowo.

Kryteria akceptacji:
- Dwukrotne kliknięcie lub przycisk "Edytuj" otwiera formularz edycji kroku
- Formularz wyświetla wszystkie parametry kroku
- Zmiany są zapisywane po kliknięciu "OK" lub "Zastosuj"
- Anulowanie zamyka formularz bez zapisywania zmian
- Edycja jest możliwa do cofnięcia (undo)

US-009: Zmiana kolejności kroków
Jako użytkownik chcę zmienić kolejność wykonywania kroków, aby dostosować przepływ automatyzacji.

Kryteria akceptacji:
- Aplikacja umożliwia przesunięcie kroku w górę lub w dół za pomocą przycisków lub przeciągania
- Przesuwanie jest możliwe tylko w ramach tego samego bloku nadrzędnego
- Przesunięcie jest natychmiast widoczne w edytorze
- Przesunięcie jest możliwe do cofnięcia (undo)

US-010: Kopiowanie i wklejanie kroków
Jako użytkownik chcę skopiować krok lub blok kroków, aby łatwo powielić złożone sekwencje.

Kryteria akceptacji:
- Aplikacja umożliwia skopiowanie wybranego kroku do schowka (Ctrl+C)
- Aplikacja umożliwia wklejenie skopiowanego kroku w wybranym miejscu (Ctrl+V)
- Aplikacja umożliwia skopiowanie całego bloku strukturalnego wraz z zawartością
- Wklejone kroki otrzymują nowe, unikalne identyfikatory
- Obrazy wzorców są również kopiowane

### 5.3 Identyfikacja elementów

US-011: Przechwytywanie koordynat myszy
Jako użytkownik chcę łatwo określić pozycję elementu na ekranie, aby nie musieć ręcznie szukać koordynat.

Kryteria akceptacji:
- Aplikacja udostępnia narzędzie "Przechwytywanie pozycji"
- Po aktywacji narzędzia aplikacja jest minimalizowana lub przeźroczysta
- Narzędzie wyświetla aktualne koordynaty kursora w czasie rzeczywistym (overlay lub małe okno)
- Kliknięcie zatwierdza wybrane koordynaty
- Koordynaty są automatycznie wstawiane do edytowanego kroku
- Narzędzie można anulować klawiszem Escape

US-012: Przechwytywanie obrazu wzorca
Jako użytkownik chcę zaznaczyć obszar ekranu jako wzorzec do rozpoznawania, aby system mógł znaleźć element niezależnie od jego dokładnej pozycji.

Kryteria akceptacji:
- Aplikacja udostępnia narzędzie "Przechwytywanie obrazu"
- Po aktywacji aplikacja robi screenshot i wyświetla go do zaznaczenia obszaru
- Użytkownik może zaznaczyć prostokątny obszar przeciągając mysz
- Narzędzie wyświetla podgląd przechwyconego obrazu
- Użytkownik może powtórzyć przechwycenie lub zatwierdzić
- Przechwycony obraz jest konwertowany do Base64 i dołączany do kroku
- Obraz wzorca jest wyświetlany jako miniatura w edytorze

US-013: Ustawianie progu dopasowania obrazu
Jako użytkownik chcę określić jak dokładne ma być dopasowanie wzorca, aby dostosować rozpoznawanie do specyfiki mojego przypadku użycia.

Kryteria akceptacji:
- Każdy krok używający rozpoznawania obrazu ma parametr "Próg dopasowania" (0-100%)
- Domyślna wartość progu to 70%
- Użytkownik może zmienić próg w formularzu edycji kroku (suwak lub pole tekstowe)
- System wyjaśnia znaczenie progu (niższy = bardziej tolerancyjny, wyższy = bardziej precyzyjny)

US-014: Wczytywanie obrazu wzorca z pliku
Jako użytkownik chcę wczytać obraz wzorca z pliku, aby móc użyć wcześniej przygotowanego wzorca.

Kryteria akceptacji:
- Formularz edycji kroku zawiera przycisk "Wczytaj obraz z pliku"
- Użytkownik może wybrać plik obrazu (PNG, JPG, BMP)
- Obraz jest konwertowany do Base64 i dołączany do kroku
- Obraz jest wyświetlany jako miniatura w edytorze
- Aplikacja informuje o błędzie, jeśli plik nie jest prawidłowym obrazem

### 5.4 Logika warunkowa

US-015: Tworzenie bloku IF
Jako użytkownik chcę dodać warunek sprawdzający obecność elementu, aby skrypt mógł reagować na różne stany interfejsu.

Kryteria akceptacji:
- Aplikacja umożliwia dodanie bloku IF z menu lub przycisku
- Blok IF zawiera warunek (element do sprawdzenia) i sekcję "Jeśli prawda"
- Użytkownik definiuje warunek poprzez wybór metody identyfikacji (koordynaty lub obraz)
- Kroki w sekcji "Jeśli prawda" są wcięte lub wizualnie pogrupowane
- Blok IF można rozwijać i zwijać

US-016: Dodawanie gałęzi ELSE
Jako użytkownik chcę dodać alternatywne działanie gdy warunek nie jest spełniony, aby obsłużyć różne scenariusze.

Kryteria akceptacji:
- Do istniejącego bloku IF można dodać sekcję ELSE
- Sekcja ELSE zawiera kroki wykonywane gdy warunek IF jest fałszywy
- Kroki w sekcji ELSE są wcięte lub wizualnie pogrupowane
- Sekcję ELSE można usunąć bez usuwania całego bloku IF

US-017: Zagnieżdżanie bloków warunkowych
Jako użytkownik chcę utworzyć warunki wewnątrz innych warunków, aby obsłużyć złożone scenariusze decyzyjne.

Kryteria akceptacji:
- Bloki IF/ELSE można dodawać wewnątrz innych bloków IF/ELSE
- Nie ma ograniczenia głębokości zagnieżdżenia
- Zagnieżdżenia są wyraźnie wizualizowane (wcięcia, linie łączące)
- Nawigacja między poziomami zagnieżdżenia jest intuicyjna

US-018: Tworzenie złożonych warunków logicznych
Jako użytkownik chcę połączyć wiele warunków operatorami logicznymi, aby stworzyć zaawansowane reguły decyzyjne.

Kryteria akceptacji:
- Warunek IF może składać się z wielu pod-warunków
- Dostępne operatory: AND, OR, NOT
- Użytkownik może dodawać, usuwać i edytować pod-warunki
- Interfejs wizualizuje strukturę złożonego warunku (drzewo lub formuła)
- Użytkownik może użyć nawiasów do grupowania warunków
- Warunek jest walidowany pod kątem poprawności składni

### 5.5 Mechanizm Obserwatora

US-019: Tworzenie Obserwatora
Jako użytkownik chcę zdefiniować Obserwatora, który będzie monitorował pojawienie się niechcianego elementu, aby automatycznie na niego zareagować.

Kryteria akceptacji:
- Aplikacja umożliwia dodanie Obserwatora w dowolnym miejscu skryptu
- Obserwator zawiera warunek (element do obserwowania) i sekwencję akcji
- Użytkownik definiuje warunek poprzez wybór metody identyfikacji (koordynaty lub obraz)
- Obserwator jest wizualnie wyróżniony w edytorze (ikona, kolor)
- Obserwator można rozwijać i zwijać

US-020: Definiowanie akcji Obserwatora
Jako użytkownik chcę określić co ma się stać gdy Obserwator wykryje element, aby zautomatyzować reakcję na zdarzenie.

Kryteria akceptacji:
- Sekcja akcji Obserwatora zawiera kroki do wykonania
- Użytkownik może dodawać dowolne typy akcji (myszy, klawiatury, przepływu)
- Akcje są wykonywane sekwencyjnie
- Akcje są wcięte lub wizualnie pogrupowane w ramach Obserwatora

US-021: Testowanie priorytetu Obserwatorów
Jako użytkownik chcę, aby system wykonał właściwego Obserwatora gdy wiele warunków jest spełnionych jednocześnie.

Kryteria akceptacji:
- System sprawdza warunki Obserwatorów według priorytetu: głębiej zagnieżdżone > wcześniej zdefiniowane
- System wykonuje tylko jednego Obserwatora na raz (pierwszy spełniony według priorytetu)
- Dokumentacja lub tooltip wyjaśnia zasady priorytetów
- W trybie debugowania użytkownik widzi który Obserwator ma aktualnie najwyższy priorytet

US-022: Obserwowanie zasięgu Obserwatora
Jako użytkownik chcę, aby Obserwator był aktywny tylko w kontekście swojego bloku, aby uniknąć niepożądanej aktywacji poza tym blokiem.

Kryteria akceptacji:
- Obserwator zdefiniowany w bloku IF jest aktywny tylko podczas wykonywania tego bloku
- Obserwator zdefiniowany na poziomie głównym jest aktywny przez cały czas wykonywania
- System dezaktywuje Obserwatora po wyjściu z jego bloku nadrzędnego
- W trybie debugowania użytkownik widzi które Obserwatorzy są obecnie aktywne

US-023: Wznowienie po akcji Obserwatora
Jako użytkownik chcę, aby skrypt wrócił do przerwane akcji po wykonaniu Obserwatora, aby kontynuować normalny przepływ.

Kryteria akceptacji:
- Po wykonaniu akcji Obserwatora system wraca do kroku, który został przerwany
- Przerwany krok jest wykonywany od początku
- Wyjątek: jeśli Obserwator wykonał akcję "Skocz do akcji", system przechodzi do wskazanego kroku

US-024: Używanie akcji "Skocz do akcji" w Obserwatorze
Jako użytkownik chcę, aby Obserwator mógł zmienić przepływ skryptu po wykryciu zdarzenia, zamiast wracać do przerwane akcji.

Kryteria akceptacji:
- Obserwator może zawierać akcję "Skocz do akcji"
- Użytkownik określa docelowy krok poprzez jego identyfikator lub etykietę
- Po wykonaniu "Skocz do akcji" system kontynuuje od wskazanego kroku (nie wraca do przerwane akcji)
- System waliduje czy docelowy krok istnieje

### 5.6 Wykonywanie skryptów

US-025: Uruchomienie skryptu
Jako użytkownik chcę uruchomić skrypt, aby rozpocząć automatyzację.

Kryteria akceptacji:
- Aplikacja zawiera wyraźny przycisk "Start" lub "Uruchom"
- Skrypt musi zawierać co najmniej jeden krok aby można go było uruchomić
- Po uruchomieniu status zmienia się na "Działający"
- Aktualnie wykonywany krok jest podświetlony w edytorze
- Przycisk "Start" jest dezaktywowany podczas wykonywania

US-026: Zatrzymanie skryptu
Jako użytkownik chcę zatrzymać działający skrypt, aby przerwać automatyzację w dowolnym momencie.

Kryteria akceptacji:
- Aplikacja zawiera wyraźny przycisk "Stop" lub "Zatrzymaj"
- Przycisk jest aktywny tylko podczas wykonywania skryptu
- Zatrzymanie następuje natychmiast (bieżąca akcja może zostać dokończona)
- Po zatrzymaniu status zmienia się na "Zatrzymany"
- Podświetlenie aktualnego kroku znika

US-027: Wstrzymanie i wznowienie skryptu
Jako użytkownik chcę wstrzymać skrypt na chwilę i potem go wznowić, aby móc tymczasowo przejąć kontrolę bez zatrzymywania całego procesu.

Kryteria akceptacji:
- Aplikacja zawiera przycisk "Pauza"
- Po kliknięciu "Pauza" wykonywanie wstrzymuje się po dokończeniu bieżącego kroku
- Status zmienia się na "Wstrzymany"
- Aplikacja zawiera przycisk "Wznów"
- Po kliknięciu "Wznów" wykonywanie kontynuuje od wstrzymanego kroku
- Status zmienia się na "Działający"

US-028: Wykonywanie w pętli
Jako użytkownik chcę, aby skrypt wykonywał się w nieskończonej pętli, aby automatyzować powtarzający się proces bez konieczności ręcznego restartowania.

Kryteria akceptacji:
- Po osiągnięciu ostatniego kroku skrypt automatycznie zaczyna od początku
- Pętla działa do momentu zatrzymania przez użytkownika
- Interfejs wyświetla licznik iteracji pętli
- Nie ma limitu liczby iteracji

US-029: Obsługa błędów podczas wykonywania
Jako użytkownik chcę, aby skrypt kontynuował działanie mimo błędów w poszczególnych krokach, aby automatyzacja nie zatrzymywała się przy każdym drobnym problemie.

Kryteria akceptacji:
- Gdy krok zwraca błąd, system loguje ten błąd szczegółowo
- System kontynuuje wykonywanie od następnego kroku
- Interfejs wyświetla informację o ostatnim błędzie (ostrzeżenie, ale nie modal)
- Użytkownik może przeglądać wszystkie błędy w panelu logów

US-030: Wizualizacja aktualnie wykonywanego kroku
Jako użytkownik chcę widzieć który krok jest aktualnie wykonywany, aby monitorować postęp automatyzacji.

Kryteria akceptacji:
- Aktualnie wykonywany krok jest podświetlony w edytorze (np. żółtym tłem)
- Jeśli krok jest w zwijniętym bloku, blok zostaje automatycznie rozwinięty
- Edytor automatycznie przewija do aktualnego kroku
- Podświetlenie jest aktualizowane w czasie rzeczywistym

### 5.7 Logowanie i debugowanie

US-031: Przeglądanie historii wykonanych akcji
Jako użytkownik chcę widzieć historię wszystkich wykonanych akcji, aby zrozumieć co dokładnie zrobił skrypt.

Kryteria akceptacji:
- Aplikacja zawiera panel historii/logów
- Każdy wpis zawiera: timestamp, nazwę kroku, typ akcji, status (sukces/błąd), czas trwania
- Historia jest aktualizowana w czasie rzeczywistym podczas wykonywania
- Historia jest posortowana chronologicznie (najnowsze na górze lub dole - konfigurowalne)
- Użytkownik może przewijać historię podczas działania skryptu

US-033: Czyszczenie historii
Jako użytkownik chcę wyczyścić historię logów, aby zacząć obserwację od nowa z czystym panelem.

Kryteria akceptacji:
- Panel logów zawiera przycisk "Wyczyść"
- Po kliknięciu wszystkie wpisy są usuwane
- Historia jest automatycznie czyszczona przy każdym nowym uruchomieniu skryptu

US-035: Włączanie trybu debugowania
Jako użytkownik chcę uruchomić skrypt w trybie debugowania, aby wykonywać go krok po kroku i obserwować szczegóły.

Kryteria akceptacji:
- Aplikacja zawiera przełącznik lub checkbox "Tryb debugowania"
- Tryb można włączyć tylko gdy skrypt jest zatrzymany
- Po włączeniu trybu i uruchomieniu skryptu, wykonywanie zatrzymuje się przed pierwszym krokiem
- Interfejs wyświetla szczegółowe informacje o następnym kroku do wykonania

US-036: Wykonywanie kroku po kroku w trybie debugowania
Jako użytkownik chcę wykonać pojedynczy krok i zobaczyć jego rezultat, aby zdiagnozować problemy w skrypcie.

Kryteria akceptacji:
- W trybie debugowania dostępny jest przycisk "Następny krok"
- Kliknięcie wykonuje jeden krok i zatrzymuje się przed następnym
- Interfejs wyświetla rezultat wykonania (sukces/błąd)
- Użytkownik może kontynuować krok po kroku lub wyłączyć tryb debugowania i kontynuować normalnie

US-037: Ustawianie punktów przerwania
Jako użytkownik chcę ustawić punkt przerwania na wybranym kroku, aby skrypt zatrzymał się w tym miejscu podczas normalnego wykonywania.

Kryteria akceptacji:
- Każdy krok może mieć ustawiony punkt przerwania (breakpoint)
- Punkt przerwania jest wizualizowany w edytorze (np. czerwona kropka)
- Podczas wykonywania skrypt zatrzymuje się przed krokiem z punktem przerwania
- Użytkownik może kontynuować wykonywanie ręcznie (jak w trybie debugowania)
- Punkty przerwania można usuwać

### 5.8 Narzędzia pomocnicze

US-039: Nadawanie etykiet krokom
Jako użytkownik chcę nadać krokowi etykietę (nazwę), aby móc łatwo się do niego odnosić w akcjach "Skocz do akcji".

Kryteria akceptacji:
- Każdy krok może mieć opcjonalne pole etykiety
- Etykieta jest wyświetlana w edytorze jako nazwa kroku (zamiast domyślnej "Krok N")
- Etykieta musi być unikalna w ramach skryptu
- System waliduje unikalność etykiet
- Etykiety są dostępne do wyboru w akcjach "Skocz do akcji"

US-040: Walidacja skryptu przed wykonaniem
Jako użytkownik chcę, aby system sprawdził poprawność skryptu przed jego uruchomieniem, aby uniknąć błędów podczas wykonywania.

Kryteria akceptacji:
- Przed uruchomieniem system waliduje strukturę skryptu
- Sprawdzane są: obecność obrazów wzorców, poprawność warunków logicznych, istnienie kroków docelowych w "Skocz do akcji"
- Jeśli walidacja wykryje błędy, użytkownik otrzymuje listę problemów
- Użytkownik może zdecydować czy uruchomić skrypt mimo ostrzeżeń (jeśli nie są krytyczne)

### 5.9 Ustawienia i konfiguracja

US-041: Konfiguracja globalnych opóźnień
Jako użytkownik chcę ustawić domyślne opóźnienia między akcjami, aby dostosować tempo wykonywania do specyfiki automatyzowanego systemu.

Kryteria akceptacji:
- Aplikacja zawiera panel ustawień
- Dostępne parametry: opóźnienie przed akcją (ms), opóźnienie po akcji (ms), opóźnienie reakcji Obserwatora (domyślnie 1000ms)
- Wartości domyślne można ustawić dla skryptu
- Poszczególne kroki mogą nadpisywać ustawienia skryptu
- Zmiany są zapisywane razem ze skryptem w pliku JSON

US-042: Konfiguracja rozpoznawania obrazu
Jako użytkownik chcę dostosować parametry rozpoznawania obrazu, aby zoptymalizować wydajność i dokładność.

Kryteria akceptacji:
- Panel ustawień zawiera sekcję rozpoznawania obrazu
- Dostępne parametry: domyślny próg dopasowania, algorytm dopasowania (jeśli dostępne warianty)
- Zmiany są zapisywane w konfiguracji aplikacji
- Poszczególne kroki mogą nadpisywać globalne ustawienia

US-043: Skróty klawiszowe
Jako użytkownik chcę korzystać ze skrótów klawiszowych, aby szybciej wykonywać najczęstsze operacje.

Kryteria akceptacji:
- Aplikacja posiada zdefiniowane skróty klawiszowe dla najczęstszych operacji
- Skróty są wyświetlane w tooltipach i menu
- Skróty są stałe i nie można ich zmieniać
- Dokumentacja zawiera pełną listę dostępnych skrótów

US-044: Zapisywanie ustawień ze skryptem
Jako użytkownik chcę, aby ustawienia specyficzne dla skryptu były zapisywane razem z nim, aby każdy skrypt mógł mieć własną konfigurację.

Kryteria akceptacji:
- Ustawienia takie jak opóźnienia, progi dopasowania obrazu są zapisywane w pliku JSON skryptu
- Przy wczytaniu skryptu ustawienia są automatycznie przywracane
- Użytkownik może edytować ustawienia dla każdego skryptu niezależnie
- Aplikacja posiada ustawienia globalne jako wartości domyślne dla nowych skryptów

### 5.10 Obsługa błędów i wyjątków

US-045: Reakcja na brak znalezienia elementu
Jako użytkownik chcę wiedzieć gdy element nie został znaleziony przez rozpoznawanie obrazu, aby móc zareagować w skrypcie.

Kryteria akceptacji:
- Gdy rozpoznawanie obrazu nie znajdzie elementu powyżej progu, zwracany jest błąd
- Błąd zawiera informację o progu dopasowania i najlepszym znalezionym dopasowaniu
- Błąd jest logowany z timestampem
- Skrypt kontynuuje od następnego kroku (zgodnie z zasadą obsługi błędów)
- W warunkach IF/Obserwatorów brak znalezienia to FALSE (nie błąd)

US-046: Reakcja na niedostępność WinAPI
Jako użytkownik chcę być poinformowany gdy aplikacja nie może uzyskać dostępu do WinAPI, aby zrozumieć dlaczego automatyzacja nie działa.

Kryteria akceptacji:
- Przy starcie aplikacja sprawdza dostępność WinAPI
- Jeśli WinAPI jest niedostępne, wyświetlany jest komunikat błędu z wyjaśnieniem
- Sugerowane rozwiązanie (uruchomienie jako administrator, sprawdzenie uprawnień)
- Aplikacja może działać w trybie edycji, ale nie może wykonywać skryptów

US-047: Obsługa błędów zapisu/odczytu plików
Jako użytkownik chcę otrzymać zrozumiały komunikat gdy wystąpi problem z zapisem lub odczytem skryptu, aby wiedzieć co zrobić.

Kryteria akceptacji:
- Przy błędach I/O wyświetlany jest dialog z komunikatem błędu
- Komunikat zawiera przyczynę (brak uprawnień, plik nie istnieje, błąd formatu)
- Użytkownik może ponowić operację lub anulować
- Dane w aplikacji nie są tracone przy błędzie zapisu

### 5.11 Wydajność i optymalizacja

US-049: Minimalizacja opóźnień sprawdzania Obserwatorów
Jako użytkownik chcę, aby sprawdzanie warunków Obserwatorów nie spowalniało wykonywania skryptu, aby automatyzacja działała płynnie.

Kryteria akceptacji:
- Obserwatorzy działają w osobnym wątku, niezależnym od głównego wątku wykonywania skryptu
- Sprawdzanie warunków Obserwatorów nie blokuje wykonywania kroków głównego skryptu
- Sprawdzanie jest zoptymalizowane (cache obrazów, szybkie dopasowanie)
- W panelu statystyk wyświetlany jest średni czas sprawdzania Obserwatorów

US-050: Wydajne rozpoznawanie obrazu
Jako użytkownik chcę, aby rozpoznawanie obrazu działało szybko, aby skrypt nie był zbyt wolny.

Kryteria akceptacji:
- Rozpoznawanie obrazu na ekranie Full HD zajmuje maksymalnie 500ms (docelowo <200ms)
- System używa optymalizacji (np. przeszukiwanie tylko zmienionego obszaru jeśli możliwe)
- W panelu statystyk wyświetlany jest średni czas rozpoznawania

US-051: Optymalizacja zużycia pamięci
Jako użytkownik chcę, aby aplikacja nie zużywała nadmiernej ilości pamięci, szczególnie przy długim wykonywaniu skryptów.

Kryteria akceptacji:
- Aplikacja zużywa maksymalnie 512 MB RAM podczas normalnej pracy
- Historia logów jest ograniczona do ostatnich 1000 wpisów aby nie zapychać pamięci
- Obrazy wzorców są efektywnie kompresowane w pamięci
- Brak wycieków pamięci po wielogodzinnym działaniu

### 5.12 Pomoc i dokumentacja

US-052: Dostęp do dokumentacji użytkownika
Jako użytkownik chcę mieć dostęp do dokumentacji, aby dowiedzieć się jak korzystać z aplikacji.

Kryteria akceptacji:
- Aplikacja zawiera menu "Pomoc" z opcją "Dokumentacja"
- Dokumentacja jest dostępna w formacie HTML lub PDF
- Dokumentacja jest wbudowana w aplikację (nie wymaga internetu)
- Dokumentacja zawiera: wprowadzenie, przewodnik krok po kroku, opis wszystkich funkcji, przykłady

US-053: Wyświetlanie tooltipów
Jako użytkownik chcę widzieć podpowiedzi po najechaniu na elementy interfejsu, aby zrozumieć ich funkcję bez czytania dokumentacji.

Kryteria akceptacji:
- Wszystkie przyciski, pola i elementy interfejsu mają tooltipsy
- Tooltipsy pojawiają się po 1 sekundzie najechania kursorem
- Tooltipsy zawierają krótki, zrozumiały opis funkcji
- Tooltipsy pokazują skrót klawiszowy (jeśli dostępny)

US-054: Przykładowe skrypty
Jako użytkownik chcę mieć dostęp do przykładowych skryptów, aby szybciej nauczyć się tworzenia własnych automatyzacji.

Kryteria akceptacji:
- Aplikacja zawiera menu "Przykłady"
- Dostępne są co najmniej 3 przykładowe skrypty o różnym poziomie złożoności
- Przykłady demonstrują: podstawowe akcje, logikę warunkową, Obserwatorów
- Użytkownik może otworzyć przykład jako nowy skrypt lub kopiować fragmenty

US-055: Okno "O aplikacji"
Jako użytkownik chcę zobaczyć informacje o aplikacji, aby poznać wersję i autora.

Kryteria akceptacji:
- Aplikacja zawiera menu "Pomoc" z opcją "O aplikacji"
- Okno wyświetla: nazwę aplikacji, wersję, informacje o licencji, dane kontaktowe/link do repozytorium
- Okno zawiera przyciski do sprawdzenia aktualizacji (jeśli funkcja dostępna) i zamknięcia

### 5.13 Dystrybucja i instalacja

US-056: Uruchamianie aplikacji bez instalacji Java (Windows)
Jako użytkownik Windows chcę uruchomić aplikację bez konieczności wcześniejszej instalacji Java, aby proces był prosty.

Kryteria akceptacji:
- Aplikacja jest dystrybuowana jako samodzielny plik .exe dla Windows
- Plik .exe zawiera osadzony JVM
- Aplikacja uruchamia się natychmiast po pobraniu i kliknięciu
- Nie wymagana jest instalacja dodatkowego oprogramowania
- Dodatkowo dostępna jest wersja .jar dla użytkowników Linux/macOS (wymaga Java 21+)

US-057: Przenośność aplikacji
Jako użytkownik chcę móc przenieść aplikację na inny komputer lub uruchomić z pendrive, bez instalacji.

Kryteria akceptacji:
- Aplikacja nie wymaga instalacji (portable)
- Wszystkie ustawienia są zapisywane w folderze aplikacji (lub w podfolderze)
- Aplikacja .exe działa po skopiowaniu na inny komputer Windows
- Aplikacja .jar działa na dowolnym systemie z zainstalowaną Java 21+
- Nie modyfikuje rejestru ani plików systemowych

US-058: Sprawdzanie wymagań systemowych przy starcie
Jako użytkownik chcę być poinformowany gdy mój system nie spełnia wymagań, zamiast doświadczać niezrozumiałych błędów.

Kryteria akceptacji:
- Przy starcie aplikacja sprawdza: wersję systemu operacyjnego, architekturę (x64), dostępną pamięć
- W przypadku wersji .jar sprawdzana jest wersja Java (minimum 21)
- Jeśli wymagania nie są spełnione, wyświetlany jest komunikat z wyjaśnieniem
- Komunikat zawiera informacje o minimalnych wymaganiach
- Użytkownik może kontynuować na własne ryzyko lub zamknąć aplikację

### 5.14 Grupowanie akcji

US-059: Grupowanie akcji w bloki
Jako użytkownik chcę pogrupować powiązane akcje w jeden blok z nazwą, aby łatwiej zarządzać złożonymi skryptami.

Kryteria akceptacji:
- Aplikacja umożliwia utworzenie bloku grupującego z własną nazwą (np. "Logowanie", "Wyszukiwanie")
- Blok grupujący może zawierać dowolną liczbę kroków i innych bloków
- Bloki grupujące można rozwijać i zwijać w edytorze
- Zawartość bloku jest wizualnie wcięta lub pogrupowana
- Blok grupujący nie wpływa na logikę wykonania - kroki wykonują się sekwencyjnie
- Użytkownik może edytować nazwę bloku, przenosić go, kopiować i usuwać
- Bloki grupujące są zapisywane w pliku JSON skryptu

## 6. Metryki sukcesu

### 6.1 Metryki funkcjonalne

6.1.1 Poprawność działania
- 100% poprawna obsługa priorytetów Obserwatorów zgodnie z zasadą: głębsze zagnieżdżenie > pierwszeństwo na tym samym poziomie
- 100% poprawna obsługa zasięgu Obserwatorów (aktywność ograniczona do bloku nadrzędnego)
- 100% poprawne działanie złożonych warunków logicznych (AND, OR, NOT) w testach walidacyjnych
- 95%+ dokładność rozpoznawania obrazu przy progu dopasowania 70% (w kontrolowanych warunkach testowych)

6.1.2 Stabilność
- Skrypt działa nieprzerwanie przez minimum 24 godziny bez crashu lub wycieku pamięci
- Aplikacja obsługuje skrypty z minimum 1000 kroków i 10 poziomami zagnieżdżenia bez problemów z wydajnością
- 99%+ kroków wykonuje się poprawnie (błędy tylko w przypadku rzeczywistych problemów z docelowym systemem)

### 6.2 Metryki wydajnościowe

6.2.1 Czas odpowiedzi
- Średni czas sprawdzania warunku Obserwatora: <100ms na cykl sprawdzania wszystkich aktywnych Obserwatorów (w osobnym wątku)
- Średni czas rozpoznawania obrazu na ekranie Full HD: <500ms (docelowo <200ms)
- Czas uruchomienia aplikacji: <5 sekund
- Czas wczytania skryptu (100 kroków, 10 obrazów): <2 sekundy
- Opóźnienie reakcji Obserwatora od momentu spełnienia warunku: konfigurowalne (domyślnie 1 sekunda)

6.2.2 Zużycie zasobów
- Zużycie pamięci RAM podczas normalnej pracy: <512 MB
- Zużycie pamięci RAM po 24h działania: wzrost <10% (brak wycieków)
- Zużycie CPU podczas bezczynności: <1%
- Zużycie CPU podczas wykonywania (bez intensywnego rozpoznawania obrazu): <15%

### 6.3 Metryki użyteczności

6.3.1 Łatwość użycia
- Nowy użytkownik jest w stanie stworzyć prosty skrypt (5 kroków, kliknięcie + tekst) w <10 minut (bez czytania dokumentacji)
- Nowy użytkownik jest w stanie stworzyć skrypt z Obserwatorem w <20 minut (po przeczytaniu krótkiego tutorial)
- 90%+ funkcji jest dostępnych w maksymalnie 2 kliknięciach z głównego ekranu

6.3.2 Satysfakcja użytkownika (planowane badanie)
- Ocena prostoty interfejsu: minimum 4/5 w ankiecie użytkowników
- Ocena stabilności działania: minimum 4/5 w ankiecie użytkowników
- Procent użytkowników, którzy poleciliby aplikację: >70%

### 6.4 Metryki jakości kodu i architektury

6.4.1 Modularność
- Separacja warstw: UI, Core Executor, Automation Layer, Vision Layer - każda w osobnych modułach
- Możliwość wymiany implementacji IElementFinder i IActionExecutor bez zmian w Core Executor
- Przygotowanie architektury pod migrację do wizualnego edytora (Opcja B): możliwość podmiany UI Layer bez zmian w Core

6.4.2 Testowalność
- Pokrycie testami jednostkowymi Core Executor: >80%
- Pokrycie testami jednostkowymi logiki Obserwatora: >90%
- Pokrycie testami integracyjnymi kluczowych scenariuszy (warunki, Obserwatorzy, pętla): 100%

### 6.5 Kluczowe Wskaźniki Sukcesu MVP

Minimalne kryteria sukcesu do uznania MVP za gotowe do wydania:
1. Wszystkie historie użytkownika z priorytetem MUST HAVE są zaimplementowane i przetestowane
2. Poprawność logiki Obserwatora: 100% (kryteria 6.1.1)
3. Stabilność: skrypt działa 24h bez problemu (kryterium 6.1.2)
4. Wydajność Obserwatorów: cykl sprawdzania <100ms, reakcja zgodna z konfiguracją (kryterium 6.2.1)
5. Aplikacja pakuje się do samodzielnego .exe i .jar oraz uruchamia bez Java (w przypadku .exe)
6. Prosty 5-krokowy skrypt tworzony w <10 minut przez nowego użytkownika (kryterium 6.3.1)

---

Koniec dokumentu PRD v1.2
Data utworzenia: 2025-11-20
Data ostatniej aktualizacji: 2025-11-21

