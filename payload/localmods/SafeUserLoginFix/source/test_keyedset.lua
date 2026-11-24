-- Offline test for SafeUserLoginFix (luajit, no game): loads Safe User Login's real Lua files twice, once alone
-- (world A) and once with this mod (world B), drives both with the same random zombie updates and lifecycle
-- events, and compares every zombie's useless flag after every update.
-- Usage: luajit test_keyedset.lua <SafeUserLogin/42/media/lua directory>

local SUL_DIR = assert(arg[1], "usage: luajit test_keyedset.lua <SafeUserLogin/42/media/lua>")
local here = (arg[0]:match("^(.*)/[^/]*$") or ".")
local FIX = here .. "/../42/media/lua/client/SafeUserLoginFix.lua"

local fails = 0
local function check(ok, what)
    print((ok and "OK   " or "FAIL ") .. what)
    if not ok then fails = fails + 1 end
end

-- Globals of PZ 42.22's Kahlua (projectzomboid.jar se/krka/kahlua/stdlib + j2se/MathLib, projectzomboid/stdlib.lua).
-- Missing compared to Lua 5.1: next, xpcall, load, loadstring, loadfile, dofile, module, newproxy, gcinfo,
-- table.maxn/getn/setn/foreachi, string.dump; never Lua 5.2+ names like table.unpack, table.pack, utf8, bit32.
local function pick(src, names)
    local t = {}
    for name in names:gmatch("%S+") do t[name] = assert(src[name], name) end
    return t
end
local function kahluaGlobals()
    local g = pick(_G, "assert error getmetatable setmetatable ipairs pairs pcall print rawequal rawget rawset select"
        .. " tonumber tostring type unpack getfenv setfenv collectgarbage")
    g.table = pick(table, "concat insert remove sort")
    g.table.isempty = function(t) for _ in pairs(t) do return false end return true end
    g.table.wipe = function(t) for k in pairs(t) do t[k] = nil end end
    g.string = pick(string, "byte char find format gsub len lower match rep reverse sub upper gmatch")
    g.math = pick(math, "abs acos asin atan atan2 ceil cos cosh deg exp floor fmod frexp ldexp log log10 max min modf"
        .. " pi pow rad random randomseed sin sinh sqrt tan tanh huge")
    g.os = pick(os, "clock date difftime time")
    g.coroutine = pick(coroutine, "create resume running status wrap yield")
    return g
end

local function readFile(path)
    local f = assert(io.open(path, "rb"))
    local s = f:read("*a")
    f:close()
    return (s:gsub("(%d)F([^%w_])", "%1%2"))   -- Kahlua float suffix (0.25F) -> plain Lua
end

------------------------------------------------------------------ one game world (mocks)
local function newWorld(withFix, bridge)
    local W = { clock = 0, handlers = {}, drawn = 0 }
    local env = kahluaGlobals()
    env._G = env
    W.log = {}

    local function eventTable(name)
        W.handlers[name] = {}
        local t = {}
        t.Add = function(fn) table.insert(W.handlers[name], fn) end
        t.Remove = function(fn)
            local list = W.handlers[name]
            for i = #list, 1, -1 do if list[i] == fn then table.remove(list, i) end end
        end
        return t
    end
    env.Events = setmetatable({}, { __index = function(t, k) local e = eventTable(k); rawset(t, k, e); return e end })
    function W.fire(name, ...)
        local list = {}
        for i, fn in ipairs(W.handlers[name] or {}) do list[i] = fn end   -- like the game: a Remove inside doesn't skip
        for _, fn in ipairs(list) do fn(...) end
    end

    env.getCore = function() return { isDedicated = function() return false end, getZoom = function() return 1 end } end
    env.getTimestampMs = function() return W.clock end
    env.HaloTextHelper = { addText = function() end, getColorRed = function() end, getColorGreen = function() end }
    env.print = function(...) W.log[#W.log + 1] = table.concat({ ... }, " ") end
    env.require = function() end
    env.isoToScreenX = function(_, x) return x * 10 end
    env.isoToScreenY = function(_, _, y) return y * 10 end
    env.getPlayerScreenLeft = function() return 0 end
    env.getPlayerScreenTop = function() return 0 end
    env.getPlayerScreenWidth = function() return 1920 end
    env.getPlayerScreenHeight = function() return 1080 end
    env.getNumActivePlayers = function() return 1 end
    env.UIFont = { Large = 1 }
    env.VALCBridge = bridge

    local base = {}
    base.__index = base
    function base:derive(name) local c = setmetatable({}, { __index = self }); c.__index = c; c.Type = name; return c end
    function base:new(x, y, w, h) return setmetatable({ x = x, y = y, w = w, h = h }, self) end
    function base:initialise() end
    function base:instantiate() end
    function base:addToUIManager() W.ui = self end
    function base:removeFromUIManager() if W.ui == self then W.ui = nil end end
    function base:setFollowGameWorld() end
    function base:setVisible() end
    function base:setStencilRect() end
    function base:clearStencilRect() end
    function base:drawText(text, x, y) W.drawn = W.drawn + 1; W.lastText = { text, x, y } end
    env.ISUIElement = base

    local files = {
        SUL_DIR .. "/shared/SafeUserLogin_shared.lua",
        SUL_DIR .. "/client/SafeUserLogin/ISTextBarManager.lua",
        SUL_DIR .. "/client/SafeUserLogin/Client.lua",
    }
    if withFix then files[#files + 1] = FIX end
    for _, path in ipairs(files) do
        local fn = assert(loadstring(readFile(path), "@" .. path))
        setfenv(fn, env)
        fn()
    end
    W.env = env
    return W
end

local function newPlayer()
    local p = { x = 100, y = 200, z = 0, moving = false, outline = false }
    function p:getX() return self.x end
    function p:getY() return self.y end
    function p:getZ() return self.z end
    function p:isAttackStarted() return false end
    function p:isPlayerMoving() return self.moving end
    function p:setOutlineHighlight(on) self.outline = on end
    function p:setOutlineHighlightCol() end
    return p
end

local function newZombies(n)
    local zs = {}
    for i = 1, n do
        local z = { id = i, useless = false, target = nil, attack = false, see = false, sets = 0 }
        function z:getVariableBoolean(name) assert(name == "bAttack"); return self.attack end
        function z:getTarget() return self.target end
        function z:CanSee(p) return self.see end
        function z:isUseless() return self.useless end
        function z:setUseless(v) self.useless = v; self.sets = self.sets + 1 end
        zs[i] = z
    end
    return zs
end

------------------------------------------------------------------ scenario
local N = 60
local A, B = newWorld(false), newWorld(true)
local pa, pb = newPlayer(), newPlayer()
local za, zb = newZombies(N), newZombies(N)

local function same(tag)
    for i = 1, N do
        if za[i].useless ~= zb[i].useless then return false, tag .. ": zombie " .. i .. " A=" .. tostring(za[i].useless) .. " B=" .. tostring(zb[i].useless) end
    end
    return true
end

local mismatches, compared = 0, 0
local firstBad
local function randomUpdates(steps, seed, pA, pB, otherA, otherB)
    math.randomseed(seed)
    for _ = 1, steps do
        local i = math.random(N)
        local r = math.random()
        local tgt = r < 0.45 and 1 or (r < 0.6 and 2 or 0)   -- 1: this player, 2: someone else, 0: nobody
        local attack = math.random() < 0.6
        local see = math.random() < 0.7
        za[i].target = tgt == 1 and pA or (tgt == 2 and otherA or nil)
        zb[i].target = tgt == 1 and pB or (tgt == 2 and otherB or nil)
        za[i].attack, zb[i].attack = attack, attack
        za[i].see, zb[i].see = see, see
        if math.random() < 0.05 then   -- something else (another mod) set the flag
            za[i].useless, zb[i].useless = true, true
        end
        A.fire("OnZombieUpdate", za[i])
        B.fire("OnZombieUpdate", zb[i])
        compared = compared + 1
        local ok, why = same("update")
        if not ok then mismatches = mismatches + 1; firstBad = firstBad or why end
    end
end

-- 1. login: protection on, player still (the countdown has not started)
A.fire("OnCreatePlayer", 0, pa)
B.fire("OnCreatePlayer", 0, pb)
local Fix = B.env.SafeUserLoginFix
check(Fix ~= nil and Fix.mode == "replaced", "B: SafeUserLogin's zombie handler caught (mode " .. tostring(Fix and Fix.mode) .. ")")
check(#B.handlers.OnZombieUpdate == 1 and B.handlers.OnZombieUpdate[1] == Fix.zombieHandler,
    "B: only this mod's handler on OnZombieUpdate (" .. #B.handlers.OnZombieUpdate .. ")")
check(#A.handlers.OnZombieUpdate == 1, "A: SafeUserLogin's own handler on OnZombieUpdate")
local other = newPlayer()
randomUpdates(20000, 7, pa, pb, other, other)
check(mismatches == 0, "while protected: useless flags equal after all " .. compared .. " updates" .. (firstBad and (" (" .. firstBad .. ")") or ""))
local setsA, setsB = 0, 0
for i = 1, N do setsA = setsA + za[i].sets; setsB = setsB + zb[i].sets end
check(setsB < setsA, "random churn: setUseless calls A " .. setsA .. ", B " .. setsB .. " (B only on change)")
-- steady horde: the same attacking zombie updated 1000 times (A flips false->true every update)
local a0, b0 = za[1].sets, zb[1].sets
za[1].target, zb[1].target, za[1].attack, zb[1].attack, za[1].see, zb[1].see = pa, pb, true, true, true, true
for _ = 1, 1000 do A.fire("OnZombieUpdate", za[1]); B.fire("OnZombieUpdate", zb[1]) end
check(za[1].useless and zb[1].useless and za[1].sets - a0 == 2000 and zb[1].sets - b0 <= 1,
    "steady attacker: setUseless A " .. (za[1].sets - a0) .. " calls, B " .. (zb[1].sets - b0))
local setSize = 0
for _ in pairs(Fix.protected[pb].useless) do setSize = setSize + 1 end
check(setSize <= N, "B: keyed set holds " .. setSize .. " zombies (bound: " .. N .. " zombies)")

-- 2. the player moves: 5 s countdown, then removeProtection -> clearZombieList
pa.moving, pb.moving = true, true
A.fire("OnPlayerUpdate", pa); B.fire("OnPlayerUpdate", pb)
for t = 0, 5200, 100 do
    A.clock, B.clock = t, t
    A.fire("OnTick", 1); B.fire("OnTick", 1)
    if t < 5000 then randomUpdates(50, 1000 + t, pa, pb, other, other) end
end
check(mismatches == 0, "during the countdown: flags still equal (" .. compared .. " updates compared)")
check(A.ui == nil and B.ui == nil, "protection ended in both (text bar removed)")
check(pa.outline == false and pb.outline == false, "outline turned off in both")
local stuckA, stuckB = 0, 0
for i = 1, N do
    if za[i].useless then stuckA = stuckA + 1 end
    if zb[i].useless then stuckB = stuckB + 1 end
end
check(stuckB == 0, "B: no zombie left useless after the protection ends")
check(stuckB <= stuckA, "A leaves " .. stuckA .. " useless (its list scan with holes misses some), B " .. stuckB)
check(next(Fix.protected) == nil and not Fix.handlerOn and #B.handlers.OnZombieUpdate == 0, "B: handler removed, nothing kept")
for i = 1, N do za[i].useless, zb[i].useless = false, false end
mismatches = 0
randomUpdates(2000, 99, pa, pb, other, other)
check(mismatches == 0, "after the protection: zombies untouched in both")
check(#A.handlers.OnZombieUpdate == 0, "A: its handler removed itself at its next update")

-- 3. respawn: new character, protection again; dies before moving -> cleanup on death
local pa2, pb2 = newPlayer(), newPlayer()
A.fire("OnCreatePlayer", 0, pa2)
B.fire("OnCreatePlayer", 0, pb2)
check(#B.handlers.OnZombieUpdate == 1 and B.handlers.OnZombieUpdate[1] == Fix.zombieHandler, "respawn: B protects again with its own handler only")
randomUpdates(5000, 11, pa2, pb2, other, other)
check(mismatches == 0, "respawn: flags equal (" .. compared .. " updates compared)")
A.fire("OnPlayerDeath", pa2); B.fire("OnPlayerDeath", pb2)
stuckB = 0
for i = 1, N do if zb[i].useless then stuckB = stuckB + 1 end end
check(stuckB == 0 and next(Fix.protected) == nil and #B.handlers.OnZombieUpdate == 0, "death: B resets its zombies and removes its handler")

-- 4. countdown text in Viewpoint 3D (bridge mock) vs isometric
local function list(...) local v = { ... }; return { size = function() return #v end, get = function(_, i) return v[i + 1] end } end
local br = { on = true, pt = list(640, 360) }
function br.active() return br.on end
function br.point() return br.pt end
function br.want() br.wanted = true end
local C = newWorld(true, br)
local pc = newPlayer()
C.fire("OnCreatePlayer", 0, pc)
local bar = C.ui
bar:render(); bar:prerender()
check(bar.locationX == 670 and bar.locationY == 340 and C.lastText[2] == 670, "3D: text placed from VALCBridge.point (+30, -20)")
check(br.wanted == true, "3D: asked the bridge for camera copies")
local drawn = C.drawn
br.pt = list()
bar:render(); bar:prerender()
check(C.drawn == drawn and bar.message ~= nil and pc.outline == true, "3D, not projectable: text hidden, message and outline kept")
br.on = false
bar:render(); bar:prerender()
check(bar.locationX == pc.x * 10 + 30 and C.drawn == drawn + 1, "isometric: original isoToScreen spot")

-- 5. an error in this mod's zombie handler: SafeUserLogin's own handler is put back, logged once, zombies reset
local D = newWorld(true)
local pd = newPlayer()
local zd = newZombies(3)
D.fire("OnCreatePlayer", 0, pd)
local FixD = D.env.SafeUserLoginFix
zd[1].target, zd[1].attack, zd[1].see = pd, true, true
D.fire("OnZombieUpdate", zd[1])
check(FixD.mode == "replaced" and zd[1].useless, "error test: protected by this mod first")
zd[2].getTarget = function() error("boom") end
D.fire("OnZombieUpdate", zd[2])
D.fire("OnZombieUpdate", zd[2])
local restored = 0
for _, line in ipairs(D.log) do if line:find("SafeUserLoginFix: error, SafeUserLogin's handler restored", 1, true) then restored = restored + 1 end end
check(FixD.mode == "failed" and restored == 1, "error: mode failed, logged once (" .. restored .. ")")
check(#D.handlers.OnZombieUpdate == 1 and D.handlers.OnZombieUpdate[1] == FixD.caught, "error: SafeUserLogin's own handler is back on OnZombieUpdate")
D.fire("OnZombieUpdate", zd[1])
check(zd[1].useless, "error: SafeUserLogin's handler protects again")
zd[3].target, zd[3].attack, zd[3].see = pd, true, true
D.fire("OnZombieUpdate", zd[3])
check(zd[3].useless, "error: new attacker made useless by SafeUserLogin's handler")

-- 6. Kahlua syntax: no goto/labels/integer division in this mod's source (comments and strings stripped)
local src = readFile(FIX):gsub("%-%-%[(=*)%[.-%]%1%]", ""):gsub("%-%-[^\n]*", ""):gsub('"[^"\n]*"', '""'):gsub("'[^'\n]*'", "''")
check(not src:find("%f[%w_]goto%f[^%w_]") and not src:find("::", 1, true) and not src:find("//", 1, true),
    "source: no goto, ::label:: or // (Lua 5.2+/LuaJIT only)")

print(fails == 0 and "ALL OK" or (fails .. " FAILED"))
os.exit(fails == 0 and 0 or 1)
