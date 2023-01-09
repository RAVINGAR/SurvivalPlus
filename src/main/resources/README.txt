## NOTES ON THE TEMPERATURE FORMULA ALGORITHM

How calculations work is such that based on where a player is, there will be an environmental temperature. This is
determined by the biomes configured temperature, the time of day, and the random modifier added on top of that.
If the player is underground and not in a cave biome, the temperature will be modified by the specified value
If the player is under-cover, meaning below a block, the default-temperature will be averaged with the environment temperature
Next stage will check if it is currently raining or storming in the player's world, this will apply the given modifier
to the last temperature only if the player is not under-cover
Then, all the blocks within the configured radius will be checked to see if it has been specified to add or subtract
to the given temperature. For each block it will be added to a total such that; temperature = total / (number of blocks / 1.5).
This is meant to basically give diminishing returns so you can just add a tonne of heat blocks and get insane heat values.
After all this calculations the final environment temperature is run through the following algorithm.

`y = ( (x - z)^3 ) / max( min( c * 100 * z, c * 4000 ), c * 500)`

where y = the change in temperature
      x = environment temperature
      z = player's current temperature
      c = calculation-scale option

https://www.desmos.com/calculator/rs0hbid5dc

In case you don't like maths, you can use the above link to view the graph and understand the values. Such that the z slider
represents the player's current temperature, whilst the c slider is the scale option (keep in mind that this must be
greater than 0.

## MMOItems Resistance Calculation

After this number is calculated. Then the player's temperature resistances are considered. The formula used provides a
number which is multiplies against the given change in temperature. The basic idea is that you can specify what is
considered the maximum resistance to temperature changes. If you attain that value then the environment will not change
the player's core temperature at all. You can also give items negative resistances which will increase the temperature
changes applied to them. An item can even have a positive cold resistance and negative heat resistance if you want
that item to only be effective in cold climates!

`y = ( z - max( min( x, z ), -z ) ) / z`

where y = the resistance modifier
      z = maximum resistance
      x = the player's resistance value (from all armour and stats)

See the formula below, where the slider z is the maximum resistance. Where x is the player's resistance
https://www.desmos.com/calculator/c3dn9xlroi

## Compatibilities

This plugin has compatibility with PlaceholderAPI, WorldGuard, MythicMobs, MMOItems

### WorldGuard
The following flag has been added to WorldGuard, it is simply called `temperature-flag`. Setting this
value will override the temperature provided by the biome. However things such as weather modifiers, block modifiers
will still be considered after the fact.

### MythicMobs
You can specify in the config.yml, various skills to be executed under temperature conditions on the player.

### MMOItems
Three stats have been added to MMOItems which can be configured on any MMOItem item. Their ids are listed below;

`cold-resistance` | `heat-resistance` | `temperature-resistance`

For these stats to show up correctly you will need to add them to the following configurations;  
`language/stats.yml` - For example `cold-resistance: '&bCold Resistance: &f<plus>{value}'`
`language/lore-format.yml` - For example, you will need to add to the list `- '#cold-resistance#'` for each of the three stats.

### PlaceholderAPI
The following placeholders should be automatically registered;

`%sp_player_temperature%` - Returns the player's current temperature  

`%sp_player_environment%` - Returns the environment's temperature based on the player's current location  

`%sp_biome_temperature_<biome>%` - Returns the current temperature of the given biome at the current time. Replace `<biome>` with a valid biome id