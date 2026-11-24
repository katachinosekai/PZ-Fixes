A-Life Viewpoint 3D Fixes 1.1.0 (mod id ALifeHeliSpot3DFix). Under a Viewpoint 3D camera:
- skips ProjectALife.HeliSpotClient.draw (isometric 2D spotlight overlay); the IsoLightSource ground light is untouched (1.0.0);
- ProjectALife.SupportClient.drawRope draws the fast-rope with VALCBridge.point (ViewpointALife) instead of the isometric projection, and draws nothing when the bridge is missing or an end is behind the camera;
- ProjectALife.EncounterCompass.placement turns the arrows by the camera heading (Viewpoint.Addons.viewState().crosshair, else VALCBridge.centre) with no isometric flattening, and hides them when there is no heading.
3D is detected with VALCBridge.active() (immediate), else A-Life's Tracer.inThreeD (250 ms). Isometric is unchanged.
