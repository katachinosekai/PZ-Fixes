-- Reactive Sound Events 3D Arrow Fix
-- ReactiveSE_SoundMarker:setAngleFromPoint turns the world offset to the sound into a
-- screen direction with the fixed isometric rotation ((dx-dy)*0.5, (dx+dy)*0.25). Under
-- Viewpoint's 3D camera the screen no longer has that orientation, so the arrow points
-- the wrong way. While the 3D view is on we rotate the offset by the camera yaw instead,
-- so "up" on the arrow is where the camera looks. Isometric keeps the original code.
--
-- Camera yaw: Viewpoint.Addons.viewState().eye.yaw is in degrees and is the world bearing
-- atan2(dy, dx) of the view direction (Viewpoint's WorldPose.direction gives world
-- forward = (cos yaw, sin yaw) * cos pitch), so camera right is (-sin yaw, cos yaw).
-- viewState() answers nil until Viewpoint has published a frame after the first ask, so
-- we fall back to the player's facing (first person looks that way) for those frames.

require "ReactiveSE/ReactiveSE_SoundMarker"

local Marker = ReactiveSE_SoundMarker
if type(Marker) ~= "table" or type(Marker.setAngleFromPoint) ~= "function" or Marker.vp3dFixed then return end

local originalSetAngle = Marker.setAngleFromPoint

local function viewpoint3D()
    local vp = Viewpoint
    if type(vp) == "table" and type(vp.Loot) == "table" and type(vp.Loot.viewOn) == "function" then
        local ok, on = pcall(vp.Loot.viewOn)
        if ok then return on == true end
    end
    local br = VALCBridge
    if br ~= nil and type(br.active) == "function" then
        local ok, on = pcall(br.active)
        if ok then return on == true end
    end
    return false
end

-- Camera forward (world x, y), read once per millisecond for all markers.
local cacheMs, cacheFx, cacheFy = -1, nil, nil

local function cameraForward(player)
    local now = getTimestampMs and getTimestampMs() or 0
    if now == cacheMs then return cacheFx, cacheFy end
    cacheMs, cacheFx, cacheFy = now, nil, nil

    local vp = Viewpoint
    local addons = type(vp) == "table" and vp.Addons or nil
    if type(addons) == "table" and type(addons.viewState) == "function" then
        local ok, state = pcall(addons.viewState)
        local eye = ok and state ~= nil and state.eye or nil
        local yaw = eye and tonumber(eye.yaw) or nil
        if yaw and state.on ~= false then
            local r = math.rad(yaw)
            cacheFx, cacheFy = math.cos(r), math.sin(r)
            return cacheFx, cacheFy
        end
    end

    -- Fallback: the player's facing (not cached: it is per player).
    cacheMs = -1
    local ok, dir = pcall(function() return player:getForwardDirection() end)
    if ok and dir then
        local fx, fy = dir:getX(), dir:getY()
        if fx * fx + fy * fy > 1e-6 then return fx, fy end
    end
    return nil, nil
end

-- self:setAngleFromPoint() is looked up through the class metatable on every call
-- (render and update), so replacing the class field reaches existing markers too.
Marker.setAngleFromPoint = function(self)
    if not self.posX or not self.posY or not self.player or not viewpoint3D() then
        return originalSetAngle(self)
    end
    local fx, fy = cameraForward(self.player)
    if not fx then return originalSetAngle(self) end

    local dx = self.posX - self.player:getX()
    local dy = self.posY - self.player:getY()
    local ahead = dx * fx + dy * fy      -- along the camera's view
    local right = dx * -fy + dy * fx     -- to the camera's right

    -- Screen offset (y down), then the same angle convention as the original.
    local radians = math.atan2(-ahead, right)
    self.angle = (math.deg(radians) + 180 + 360) % 360
end
Marker.vp3dFixed = true
