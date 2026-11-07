# Варіант 32: фітнес-трекер добового енергобалансу

Домен: fitness_calories. Файл журналу: `testdata/variant_32_fitness_calories.csv` (500 000 рядків, seed і розподіл помилок у `testdata/variant_32_fitness_calories.manifest.json`). Для швидкого тесту використано перші 1000 рядків: `testdata/sample_32.csv`.

Генерація (з репозиторію курсу):

    cd n04_state_event_logs/scripts
    python3 generate_event_logs.py --variant 32 --rows 500000

Запуск і тести:

    sbt test
    sbt run
    sbt "run testdata/sample_32.csv"

Структура:

- `domain.scala` - ADT помилок і подій, smart constructors, стан і fold-крок
- `Parser.scala` - рядок CSV у `Either[ParseError, Event]`
- `Importer.scala` - `partitionMap` по всьому файлу, потім `foldLeft` у стан
- `Goal.scala` - прогрес до цілі (дефіцит 500 ккал)
- `Main.scala` - звіт у консоль

Припущення: у журналі немає міток часу, тому весь файл вважається однією добою. Нетто-баланс = спожито - спалено (тренування + базовий метаболізм) + сума коригувань зважування. Інваріант варіанта (`analytics_goal_tracking`) - відстеження цілі: фактичний дефіцит = -нетто-баланс, ціль досягнута при дефіциті >= 500.
