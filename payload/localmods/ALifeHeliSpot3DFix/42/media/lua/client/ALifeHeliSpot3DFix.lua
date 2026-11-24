-- A-Life Viewpoint 3D Fixes (mod id ALifeHeliSpot3DFix)
-- Project A-Life draws a few overlays on OnPreUIDraw with the isometric projection only.
-- Under a Viewpoint 3D camera (first or third person) they land in the wrong place:
--  1. ALifeHeliSpotClient.draw: the helicopter spotlight overlay (glow diamonds, faint cone,
--     ground pool) shows as a flat 2D triangle. Hidden in 3D, like A-Life's tracers and the
--     support searchlight beam. The ground light (IsoLightSource) is not touched.
--  2. ALifeSupportClient.drawRope: the support heli's fast-rope. Drawn in 3D with points
--     projected through Viewpoint's camera (VALCBridge, from Viewpoint x A-LIFE); skipped in 3D
--     when the bridge is missing or an end is off screen. Never an isometric line in 3D.
--  3. ALifeEncounterCompass.placement: the encounter arrows assume the isometric camera
--     (45 degree turn, flattened). In 3D they turn with the camera: up = where it looks.
-- The isometric view is unchanged. No A-Life files are changed.

require "ProjectALife/Combat/ALifeTracer"
require "ProjectALife/World/ALifeHeliSpotClient"
require "ProjectALife/World/ALifeSupportClient"
require "ProjectALife/Interface/ALifeEncounterCompass"

local Fix = {
    asker = "ALifeHeliSpot3DFix",
    wanted = false,
    metrics = { ropeDrawn = 0, ropeSkipped = 0, compass3d = 0, compassSkipped = 0 },
}
ALifeViewpoint3DFix = Fix

local function nowMs()
    if type(getTimestampMs) == "function" then return getTimestampMs() end
    return 0
end

-- VALCBridge (ViewpointALife's Java side) or nil
local function bridge()
    local br = VALCBridge
    if br == nil or type(br.active) ~= "function" or type(br.point) ~= "function" then return nil end
    return br
end

-- Is a Viewpoint 3D camera on right now? VALCBridge.active() reads Viewpoint's View.enabled
-- directly, so this follows a view switch on the same frame. Without the bridge (or in split
-- screen, where A-Life turns its own 3D handling off) A-Life's Tracer.inThreeD is used, which
-- re-checks only every 250 ms.
local function threeD(tracer)
    local split = type(getNumActivePlayers) == "function" and getNumActivePlayers() > 1
    local br = bridge()
    if br ~= nil and not split then
        local ok, on = pcall(br.active)
        if ok then return on == true end
    end
    if type(tracer) ~= "table" or type(tracer.inThreeD) ~= "function" then return false end
    local ok, value = pcall(tracer.inThreeD, nowMs())
    return ok and value == true
end
Fix.threeD = threeD

-- Ask the bridge to copy Viewpoint's camera after each frame (it only does so while someone
-- asks, and only while the 3D view is on). Asked once, the first time it's needed.
local function want(br)
    if Fix.wanted or type(br.want) ~= "function" then return end
    Fix.wanted = true
    pcall(br.want, Fix.asker, true)
end

-- world tile x, y and z in storeys -> screen pixels (the draw space at OnPreUIDraw), or nil
-- when behind the camera / no fresh camera copy (the bridge then returns an empty list)
local function project(br, x, y, z)
    local ok, q = pcall(br.point, x, y, z)
    if not ok or q == nil or q:size() < 2 then return nil end
    return q:get(0), q:get(1)
end

------------------------------------------------------------------ 1. heli spotlight overlay
local Spot = ProjectALife and ProjectALife.HeliSpotClient
if type(Spot) == "table" and type(Spot.draw) == "function" and not Spot.vp3dFixed then
    local originalDraw = Spot.draw
    -- render() looks Spot.draw up per call (pcall(Client.draw, ...)), so replacing the field is enough.
    Spot.draw = function(tracer, state, flight, profile, tex, u, v)
        if type(tracer) == "table" and tracer.projector == nil and threeD(tracer) then
            Spot.metrics.hidden3d = (Spot.metrics.hidden3d or 0) + 1
            return
        end
        return originalDraw(tracer, state, flight, profile, tex, u, v)
    end
    Spot.vp3dFixed = true
end

------------------------------------------------------------------ 2. support heli fast-rope
local Support = ProjectALife and ProjectALife.SupportClient
if type(Support) == "table" and type(Support.drawRope) == "function" and not Support.vp3dFixed then
    local originalRope = Support.drawRope

    -- same rope as A-Life's drawRope, ends projected through the 3D camera; false: not drawn
    local function drawRope3D(tracer, heli, top, bottom)
        local br = bridge()
        if br == nil then return false end
        want(br)
        local square = getCell():getGridSquare(math.floor(heli.x), math.floor(heli.y), math.floor(heli.z))
        if not square or not square:isOutside() then return false end
        local tile = tracer.whiteTile()
        if tile == nil then return false end
        local x, y = heli.x + 0.5, heli.y + 0.5
        local topX, topY = project(br, x, y, heli.z + top)
        local footX, footY = project(br, x, y, heli.z + bottom)
        if topX == nil or footX == nil then return false end
        local renderer = getRenderer()
        for _, strand in ipairs(Support.ropeStrands) do
            local c = strand.colour
            renderer:renderline(tile, topX + strand.shift, topY, footX + strand.shift, footY, c[1], c[2], c[3], 0.95)
        end
        return true
    end

    -- renderRope (the OnPreUIDraw handler A-Life registered by value) calls
    -- pcall(Client.drawRope, ...) through the table, so replacing the field is enough.
    Support.drawRope = function(tracer, heli, top, bottom)
        if type(tracer) ~= "table" or tracer.projector ~= nil or not threeD(tracer) then
            return originalRope(tracer, heli, top, bottom)
        end
        if drawRope3D(tracer, heli, top, bottom) then
            Fix.metrics.ropeDrawn = Fix.metrics.ropeDrawn + 1
        else
            Fix.metrics.ropeSkipped = Fix.metrics.ropeSkipped + 1
        end
    end
    Support.vp3dFixed = true
end

------------------------------------------------------------------ 3. encounter compass
local Compass = ProjectALife and ProjectALife.EncounterCompass
if type(Compass) == "table" and type(Compass.placement) == "function" and not Compass.vp3dFixed then
    local originalPlacement = Compass.placement
    local facing = { at = nil, fx = nil, fy = nil }

    -- where the 3D camera looks, as a unit vector in world tiles (x east, y south), or nil
    local function cameraFacing()
        local now = nowMs()
        if facing.at == now then return facing.fx, facing.fy end   -- one lookup per frame for all arrows
        facing.at, facing.fx, facing.fy = now, nil, nil
        local fx, fy
        -- Viewpoint's add-on view feed: the crosshair ray's direction, already in world tiles
        local addons = Viewpoint ~= nil and Viewpoint.Addons or nil
        if addons ~= nil and type(addons.viewState) == "function" then
            local ok, view = pcall(addons.viewState)
            local ray = ok and type(view) == "table" and view.on == true and view.crosshair or nil
            if type(ray) == "table" then fx, fy = tonumber(ray.dx), tonumber(ray.dy) end
        end
        -- else: where the screen centre meets the player's floor, through VALCBridge
        if fx == nil or fy == nil then
            fx, fy = nil, nil
            local br = bridge()
            local player = getSpecificPlayer(Compass.state.playerNum or 0)
            if br ~= nil and type(br.centre) == "function" and player ~= nil then
                want(br)
                local ok, q = pcall(br.centre, player:getZ())
                if ok and q ~= nil and q:size() >= 2 then
                    fx, fy = q:get(0) - player:getX(), q:get(1) - player:getY()
                end
            end
        end
        if fx == nil then return nil end
        local length = math.sqrt(fx * fx + fy * fy)
        if length < 0.05 then return nil end   -- looking straight down: no heading
        facing.fx, facing.fy = fx / length, fy / length
        return facing.fx, facing.fy
    end

    -- drawArrow calls Compass.placement through the table, so replacing the field is enough.
    Compass.placement = function(screenW, screenH, dx, dy)
        if not threeD(ProjectALife.Tracer) then return originalPlacement(screenW, screenH, dx, dy) end
        local length = math.sqrt(dx * dx + dy * dy)
        if length < 0.001 then return nil end
        local fx, fy = cameraFacing()
        if fx == nil then
            Fix.metrics.compassSkipped = Fix.metrics.compassSkipped + 1
            return nil   -- no heading: no arrow rather than an isometric one
        end
        -- camera right is (-fy, fx) in world tiles; screen up is camera forward, no flattening
        local sx = (dx * -fy + dy * fx) / length
        local sy = -(dx * fx + dy * fy) / length
        local halfW, halfH = screenW * 0.5, screenH * 0.5
        local radius = math.min(halfW, halfH) - Compass.state.settings.margin
        Fix.metrics.compass3d = Fix.metrics.compass3d + 1
        return halfW + sx * radius, halfH + sy * radius, sx, sy
    end
    Compass.vp3dFixed = true
end
