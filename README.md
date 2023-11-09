# MonoBudget

Self-hosted app to automatically import transactions from
Ukrainian digital bank [Monobank](https://www.monobank.ua/) into one of the budgeting apps:

- [YNAB](https://www.youneedabudget.com/)
- [Lunchmoney](https://lunchmoney.app)

### Features

- Creates transactions in the budgeting app as they happen (via Monobank
  API [webHook](https://api.monobank.ua/docs/#tag/Kliyentski-personalni-dani/paths/~1personal~1webhook/post)).
- Supports multiple monobank accounts and links transactions to the corresponding accounts created in the budgeting app.
- Tries to guess transaction's category using [MCC](https://en.wikipedia.org/wiki/Merchant_category_code) codes.
- Automatically recognizes transfers between monobank accounts and creates transfers in the budgeting app.
- Uses Telegram bot to notify about created transactions in the budgeting app.
- Telegram bot allows to change incorrectly assigned or empty category directly in Telegram messenger.

### Preparations

1. Get API token for each of the Monobank accounts you want to use as
   described [here](https://api.monobank.ua/docs/#tag/Kliyentski-personalni-dani).
2. Get API token for the budgeting app:
    - [Article](https://api.youneedabudget.com) on how obtain token for YNAB.
    - [Docs](https://lunchmoney.dev/#authentication) for Lunchmoney.
3. Create Telegram bot and obtain its token as described [here](https://core.telegram.org/bots#how-do-i-create-a-bot).
4. Create a file `settings.yml` with the following structure:
    ```yaml
    budgetBackend: !<budgeting-app>
      token: 
    bot:
      token: 
      username: 
    mono:
      settings:
        - accountId: 
          token: 
          alias: 
          budgetAccountId: 
          telegramChatId: 
    mcc:
      mccGroupToCategoryName:
      mccToCategoryName:
    ```
   description:
    - `!<budgeting-app>` may be either `!<lunchmoney>` or `!<ynab>`
    - `bot.token` is a token for the Telegram bot obtained in step 3
    - `bot.username` is a username of the Telegram bot without `@`
    - `mono.settings` is an array with settings for the monobank accounts you want to track and import transactions from
        - `accountId` is an id of the monobank account
        - `token` is a token for that account obtained in step 1
        - `alias` is a user-friendly alias for the account (mostly for logs)
        - `budgetAccountId` is an id of the account in the budgeting app where transactions from this Monobank account
          will be created at
        - `telegramChatId` is a chatId of the Telegram user where notifications about created transactions will be sent
          to
    - `mcc` is special object where you can configure how to map MCC codes in the transactions to the categories you use
      in your budgeting app
      For example:
         ```yaml
           mcc:
             mccGroupToCategoryName:
               CLS: Одяг
               HR: Розваги
               ES: Розваги
               US: Комунальні
             mccToCategoryName:
               '4011': Транспорт
               '5941': Одяг
               '5211': Товари для дому
               '5441': Ресторани
               '5912': Здоров'я
               '5977': Догляд за собою
               '5992': Подарунки
               '5995': Домашні тварини
               '5541': Машина
               '5947': Розваги
         ```

### Setup

Preferred way of using the app is via Docker image `marchukd/monobudget`:

```bash
docker run marchud/monobudget:latest -v /path/to/settings.yml:/opt/app/settings.yml -e ...
```

There are a couple of environment variables you must set:

- `MONO_WEBHOOK_URL`: URL which Monobank servers will call to pass a new transaction.
  The app takes the `path` component of this URL and creates a web-server listening at `http://localhost:PORT/PATH`.
  If you want HTTPS support then you might prefer to use a reverse-proxy like `nginx` or create a PR.
- `WEBHOOK_PORT`: port at which webserver for the webhook is listening at
- `SET_WEBHOOK`: `true` or `false`, whether to
  call [Monobank API](https://api.monobank.ua/docs/#tag/Kliyentski-personalni-dani/paths/~1personal~1webhook/post)
  setting webhook at application startup or skip this step (i.e. webhook was set previously and URL hasn't changed)

### About YNAB support

Previously this app supported only YNAB as this was my preferred financial manager. Then I switched to Lunchmoney and refactored the app to support both, but I only tested YNAB support with the minimal effort after that, and I am not confident it works reliably.

Last commit where YNAB support was working reliably (and I was personally using it)
is [3a7da7af](https://github.com/smaugfm/monobudget/commit/3a7da7afd85bffa310f54a322c46d626d24f488c) (May 2022)

PRs are feature requests are welcome!
