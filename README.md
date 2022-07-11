# Squeeth Discord Bot
A Discord bot based on [JDA](https://github.com/DV8FromTheWorld/JDA) that provides you with quick and easy access to info of the [Squeeth](https://squeeth.com) ecosystem

[**Invite the Squeeth Discord Bot to your server!**](https://discord.com/oauth2/authorize?client_id=966062130472304681&permissions=0&scope=applications.commands%20bot)

# How to Use

Run `/help` to get a list of commands in a server it is in OR in the bot's DMs!

If you're a server owner/admin, run `/settings` to see how to configure your server

# Features
Near real-time data of Squeeth by [Laevitas](https://app.laevitas.ch/dashboard/squeeth)
- Live Statistics
  - Price
  - Volume
  - Funding
  - Implied Volatility
  - Normalization Factor
- The Greeks
- Funding Calculator
- Crab Stats
- Positions Viewer
  - Long squeeth and Crab supported
  - Shorts are not supported yet
- Short Vault Data
  - See collateral, debt, collateralization ratio, NFT collateral, and vault greeks
- More Commands Soon:tm:
  - Squiz (Squeeth Quiz)
  - :eyes:

# Requirements to Self-Host

- Java 8+
- Discord Bot Token
- Laevitas API Key
- Ethereum RPC Node (self-hosted or 3rd party, preferably Alchemy)
- A Computer That Doesn't Explode and Can Run 24/7 (hopefully)

When self-hosting, pass the path to the settings json and configure the JSON for your needs.

Example: `java -jar SqueethDiscordBot.jar settings.json`

A sample settings.json can be found [here](./storage/settings_EXAMPLE.json)

# Contact

If you questions, ideas, concerns, etc., feel free to leave them at [Issues tab](https://github.com/AlphaSerpentis/SqueethDiscordBot/issues) or reach out to me (AlphaSerpentis#3203) at the [Opyn Discord](https://discord.gg/opyn) 
