-- Safe User Login Fix (mod id SafeUserLoginFix), for Safe User Login (mod id SafeUserLogin, Workshop 3232993626).
--
-- 1. Zombie handler. SafeUserLogin's OnZombieUpdate (Client.lua:101-143) runs for every zombie update while a login
--    protection is on: it scans the player's zombiesList linearly (a list with nil holes, so `#` is unreliable and
--    the list keeps growing with duplicates), and calls setUseless(false) and then often setUseless(true) on every
--    update. Here the same rule runs with a keyed set per player (O(1), no holes, no growth) and setUseless is
--    called only when the value changes. Same protection: it starts and ends where SafeUserLogin's does (its text bar
--    is created at OnCreatePlayer and removed in removeProtection, also on death), the same zombies become useless
--    (the ones that can see the player, target the player and are attacking), a zombie leaves the set when it can no
--    longer see the player, and at the end every zombie in the set that is still useless is reset (its clearZombieList).
--    Seam: SafeUserLogin's functions are locals, but it calls its helpers through the global table
--    SafeUserLogin_mod.Utils (createTextBar / removeTextBar) and registers its zombie handler with
--    Events.OnZombieUpdate.Add right after createTextBar, inside its OnCreatePlayer. For that one call the Add is
--    wrapped, the handler is kept out of the event and this file's handler is used instead; SafeUserLogin then never
--    registers it again (its onZombieAdded flag stays true because its handler never runs). If the handler can't be
--    caught that way, nothing is replaced and SafeUserLogin runs as before.
-- 2. Countdown text under Viewpoint's 3D camera: the bar is placed with isoToScreenX/Y (isometric). In 3D it is placed
--    above the player through Viewpoint x A-LIFE's camera bridge (VALCBridge.point) and hidden when that can't project
--    it (bridge missing, first person head behind the camera). Isometric and split screen are unchanged.
-- 3. Errors: any error in this file's zombie path (handler, set reset, handler sync) puts SafeUserLogin's own caught
--    handler back on OnZombieUpdate and logs "SafeUserLoginFix: error, SafeUserLogin's handler restored" once; an error
--    while placing the 3D countdown keeps the isometric spot. Kahlua (PZ's Lua) has no next/xpcall/loadstring/goto:
--    source/test_keyedset.lua runs this file with only Kahlua's globals.
-- The player outline is left as is: the flicker in 3D is fixed for all mods by ViewpointOutlineFix (Java).

if getCore():isDedicated() then return end

require "SafeUserLogin/Client"
require "SafeUserLogin/ISTextBarManager"

local SUL = SafeUserLogin_mod
local Utils = SUL and SUL.Utils
if type(Utils) ~= "table" or type(Utils.createTextBar) ~= "function" or type(Utils.removeTextBar) ~= "function"
        or type(Utils.getTextBar) ~= "function" or Utils.safeUserLoginFix then
    print("SafeUserLoginFix: SafeUserLogin not found or changed, nothing done")
    return
end

local Fix = {
    mode = "pending",   -- "replaced": SafeUserLogin's zombie handler caught; "passive": not caught, SafeUserLogin runs as is;
                        -- "failed": an error in this file's zombie path, SafeUserLogin's handler put back (see fail)
    protected = {},     -- player -> { useless = { [zombie] = true } }
    handlerOn = false,
    metrics = { updates = 0, setUseless = 0 },
}
SafeUserLoginFix = Fix

-------------------------------------------------------------------------------- 1. zombie handler
-- One zombie update, for every protected player. Same result as SafeUserLogin's OnZombieUpdate:
-- useless = can see the player and targets the player and is attacking; joins the player's set then;
-- leaves it when it can't see the player. CanSee (a line-of-sight test) is only asked when it can change something.
local function onZombieUpdate(zombie)
    if not zombie then return end
    Fix.metrics.updates = Fix.metrics.updates + 1
    local want = false
    local attack
    for player, state in pairs(Fix.protected) do
        local set = state.useless
        local inSet = set[zombie] == true
        local cond = false
        if zombie:getTarget() == player then
            if attack == nil then attack = zombie:getVariableBoolean("bAttack") == true end
            cond = attack
        end
        if cond or inSet then
            if zombie:CanSee(player) then
                if cond then
                    want = true
                    if not inSet then set[zombie] = true end
                end
            elseif inSet then
                set[zombie] = nil
            end
        end
    end
    if zombie:isUseless() ~= want then
        zombie:setUseless(want)
        Fix.metrics.setUseless = Fix.metrics.setUseless + 1
    end
end
Fix.onZombieUpdate = onZombieUpdate

local addZombie = Events.OnZombieUpdate.Add       -- the event's own Add/Remove, kept before any wrapping
local removeZombie = Events.OnZombieUpdate.Remove

-- Kahlua has no next(): "is anyone protected" by pairs.
local function anyProtected()
    for _ in pairs(Fix.protected) do return true end
    return false
end

-- Any error in this file's zombie path: hand the job back to SafeUserLogin's own handler (once) and stay passive.
-- Zombies this file made useless are reset; SafeUserLogin's handler decides them again at their next update.
local function fail(err)
    if Fix.mode == "failed" then return end
    Fix.mode = "failed"
    Fix.error = tostring(err)
    if Fix.handlerOn then
        Fix.handlerOn = false
        pcall(removeZombie, Fix.zombieHandler)
    end
    local protected = Fix.protected
    Fix.protected = {}
    for _, state in pairs(protected) do
        for zombie in pairs(state.useless) do
            pcall(function() if zombie:isUseless() then zombie:setUseless(false) end end)
        end
    end
    if Fix.caught then pcall(addZombie, Fix.caught) end
    print("SafeUserLoginFix: error, SafeUserLogin's handler restored (" .. Fix.error .. ")")
end
Fix.fail = fail

-- the function registered on OnZombieUpdate: any error -> fail()
local function zombieHandler(zombie)
    local ok, err = pcall(onZombieUpdate, zombie)
    if not ok then fail(err) end
end
Fix.zombieHandler = zombieHandler

local function syncHandlerRaw()
    local want = Fix.mode == "replaced" and anyProtected()
    if want and not Fix.handlerOn then
        addZombie(zombieHandler)
        Fix.handlerOn = true
    elseif not want and Fix.handlerOn then
        removeZombie(zombieHandler)
        Fix.handlerOn = false
    end
end

local function syncHandler()
    local ok, err = pcall(syncHandlerRaw)
    if not ok then fail(err) end
end

-- Catching SafeUserLogin's handler: armed by its first createTextBar, the next Events.OnZombieUpdate.Add in the same
-- OnCreatePlayer is its own (Client.lua:181-185). Disarmed by this file's OnCreatePlayer handler, which runs right after
-- SafeUserLogin's, or at the next tick.
local armed = false
local function disarm()
    if not armed then return end
    armed = false
    if Events.OnZombieUpdate.Add ~= addZombie then Events.OnZombieUpdate.Add = addZombie end
    if Fix.mode == "pending" then
        Fix.mode = "passive"
        Fix.protected = {}
        print("SafeUserLoginFix: SafeUserLogin's zombie handler was not caught, it runs unchanged")
    end
end

local function arm()
    armed = true
    Events.OnZombieUpdate.Add = function(fn, ...)
        if armed and Fix.mode == "pending" and type(fn) == "function" and fn ~= zombieHandler then
            armed = false
            Events.OnZombieUpdate.Add = addZombie
            Fix.mode = "replaced"
            Fix.caught = fn
            print("SafeUserLoginFix: SafeUserLogin's zombie handler replaced (keyed set, setUseless only on change)")
            syncHandler()
            return
        end
        return addZombie(fn, ...)
    end
    if Events.OnZombieUpdate.Add == addZombie then armed = false; Fix.mode = "passive" end   -- table not writable
end

-------------------------------------------------------------------------------- 2. 3D countdown text
local HEAD = 0.75   -- storeys above the feet

local function bridge()
    local br = VALCBridge
    if br == nil or type(br.active) ~= "function" or type(br.point) ~= "function" then return nil end
    return br
end

local function viewpoint3D()
    if type(getNumActivePlayers) == "function" and getNumActivePlayers() > 1 then return false end
    local br = bridge()
    if br ~= nil then
        local ok, on = pcall(br.active)
        if ok then return on == true end
    end
    local vp = Viewpoint
    if type(vp) == "table" and type(vp.Loot) == "table" and type(vp.Loot.viewOn) == "function" then
        local ok, on = pcall(vp.Loot.viewOn)
        return ok and on == true
    end
    return false
end
Fix.viewpoint3D = viewpoint3D

local wanted = false
local function project(x, y, z)
    local br = bridge()
    if br == nil then return nil end
    if not wanted and type(br.want) == "function" then
        wanted = true
        pcall(br.want, "SafeUserLoginFix", true)
    end
    local ok, q = pcall(br.point, x, y, z)
    if not ok or q == nil or q:size() < 2 then return nil end
    return q:get(0), q:get(1)
end

-- after the bar's own update/render: in 3D, replace its isometric spot (same +30/-20 px offset)
local function place3D(bar)
    if not viewpoint3D() then bar.vp3dHidden = false return end
    local p = bar.player
    local sx, sy
    if p then sx, sy = project(p:getX(), p:getY(), p:getZ() + HEAD) end
    if sx then
        bar.locationX = sx + 30
        bar.locationY = sy - 20
        bar.vp3dHidden = false
    else
        bar.vp3dHidden = true
    end
end

-- an error while placing: stop placing (the bar keeps its isometric spot), log once
local placeOff = false
local function place3DSafe(bar)
    if placeOff then return end
    local ok, err = pcall(place3D, bar)
    if not ok then
        placeOff = true
        bar.vp3dHidden = false
        print("SafeUserLoginFix: error placing the countdown in 3D, isometric spot kept (" .. tostring(err) .. ")")
    end
end

local function patchBarClass(bar)
    local cls = getmetatable(bar)
    if type(cls) ~= "table" or cls.safeUserLoginFix then return end
    local update, render, prerender = cls.update, cls.render, cls.prerender
    if type(update) ~= "function" or type(render) ~= "function" or type(prerender) ~= "function" then return end
    cls.safeUserLoginFix = true
    cls.update = function(self)
        update(self)
        place3DSafe(self)
    end
    cls.render = function(self)
        render(self)
        place3DSafe(self)
    end
    cls.prerender = function(self)
        if not self.vp3dHidden then return prerender(self) end
        local message = self.message   -- no spot in 3D: everything but the text
        self.message = nil
        prerender(self)
        self.message = message
    end
end

-------------------------------------------------------------------------------- SafeUserLogin's helpers
local createTextBar, removeTextBar = Utils.createTextBar, Utils.removeTextBar

Utils.createTextBar = function(player, playerIndex)
    local ok = createTextBar(player, playerIndex)
    if not ok or not player then return ok end
    local bar = Utils.getTextBar(player)
    if bar then patchBarClass(bar) end
    if Fix.mode == "pending" or Fix.mode == "replaced" then
        Fix.protected[player] = { useless = {} }
        if Fix.mode == "pending" then arm() else syncHandler() end
    end
    return ok
end

-- removeProtection (end of the countdown, or death) calls removeTextBar and then its clearZombieList, which has
-- nothing to do now (its list stays empty): the set is cleared here, in the same call.
Utils.removeTextBar = function(player)
    removeTextBar(player)
    local state = player and Fix.protected[player]
    if not state then return end
    Fix.protected[player] = nil
    local ok, err = pcall(function()
        for zombie in pairs(state.useless) do
            if zombie:isUseless() then zombie:setUseless(false) end
        end
    end)
    if not ok then fail(err) return end
    syncHandler()
end
Utils.safeUserLoginFix = true

Events.OnCreatePlayer.Add(function() disarm() end)
local function firstTicks()
    disarm()
    if Fix.mode ~= "pending" then Events.OnTick.Remove(firstTicks) end
end
Events.OnTick.Add(firstTicks)
