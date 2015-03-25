(ns hawkthorne.core
  (:require [hawkthorne.config :as config]))

(defn GameState [game]
  (reify
    Object
    (create [this]
            (set! (.-map this) (.tilemap (.. game -add) "hallway"))
            ;the first parameter is the tileset name as specified in Tiled, the second is the key to the asset
            (.. this -map (addTilesetImage "greendale-interior" "greendale-interior"))

            (doseq [x ["background" "accents" "floor"]]
              (aset this x (.. this -map (createLayer x))))

            (.. this -map (setCollisionBetween 15 15 true "floor"))

            ;resizes the game world to match the layer dimensions
            (.. this -background (resizeWorld))

            (set! (.-player this) (.sprite (.. game -add) 0 0 "abed"))

            (set! (.. this -player -smoothed) false)
            (.. this -player -animations (add "left"
                                              #js["1,2" "1,3" "1,4" "1,3"]
                                              10 true))
            (.. this -player -animations (add "right"
                                              #js["2,2" "2,3" "2,4" "2,3"]
                                              10 true))

            (.startSystem (.. game -physics) js/Phaser.Physics.ARCADE)
            (.enable (.. game -physics -arcade) (.-player this))

            (.. game -camera (follow (.-player this)))

            (set! (.. this -player -body -collideWorldBounds) true)

            (.setTo (.. this -player -body -maxVelocity) config/max_x_speed)

            (.setTo (.. this -player -body -drag) config/friction 0)

            (set! (.. game -physics -arcade -gravity -y) config/gravity)

            (set! (.-canDoubleJump this) false)

            (.addKeyCapture (.. game -input -keyboard)
                            (clj->js
                             '(js/Phaser.Keyboard.LEFT
                               js/Phaser.Keyboard.RIGHT
                               js/Phaser.Keyboard.SPACEBAR
                               js/Phaser.Keyboard.DOWN)))

            (set! (.. game -time -advancedTiming) true)
            (set! (.-fpsText this) (.text (.. game -add) 20 20 "Init"
                                          (clj->js {:font "16px Arial"
                                                    :fill "#ffffff"})))

            ;Prevent aliasing when scaling
            (.setImageRenderingCrisp js/Phaser.Canvas (.. game -canvas))

            (set! (.. this -fpsText -fixedToCamera) true))

    (preload [this]
             (let [r (/ 480 320)]
               (set! (.. this -scale -maxWidth) 960)
               (set! (.. this -scale -maxHeight) 640)
               (set! (.. this -scale -pageAlignVertically) true)
               (set! (.. this -scale -pageAlignHorizontally) true)
               (set! (.. this -scale -scaleMode) js/Phaser.ScaleManager.SHOW_ALL)
               (.. this -scale (setScreenSize))

               (.tilemap (.-load this) "hallway" "/assets/maps/hallway.json" nil, js/Phaser.Tilemap.TILED_JSON)
               (.image (.-load game) "greendale-interior" "/assets/maps/greendale-interior.png")
               (.image (.-load game) "player" "/assets/gfx/player.png")
               (.atlasJSONHash (.-load game) "abed" "/assets/abed.png" "/assets/player.json")))

    (update [this]
            (if (not= 0 (.. game -time -fps))
              (.setText (.-fpsText this) (str (.. game -time -fps) "FPS")))

            (.collide (.. game -physics -arcade) (.-player this) (.-floor this))

            (set! (.. this -player -body -acceleration -x)
                  (cond
                   (.leftInputIsActive this) (- config/acceleration)
                   (.rightInputIsActive this) config/acceleration
                   :else 0))

            (if (> 0 (.. this -player -body -velocity -x))
              (.. this -player -animations (play "left")))

            (if (< 0 (.. this -player -body -velocity -x))
              (.. this -player -animations (play "right")))

            (if (= 0 (.. this -player -body -velocity -x))
              (do (.. this -player -animations stop)
                (set! (.. this -player -frame) 0)))

            (let [onTheGround (.. this -player -body -blocked -down)]
              (if (and onTheGround (.upInputIsActive this 5))
                (set! (.. this -player -body -velocity -y) config/jump_speed))))

    (leftInputIsActive [this]
                       (let [isActive (.isDown (.. this -input -keyboard) js/Phaser.Keyboard.LEFT)
                             inPlace (not (and (.. game -input -activePointer -isDown)
                                               (< (.. game -input -activePointer -x)
                                                  (/ (.-width game) 4))))]
                         (= isActive inPlace)))

    (rightInputIsActive [this]
                        (let [width (.-width game)
                              isActive (.isDown (.. this -input -keyboard) js/Phaser.Keyboard.RIGHT)
                              inPlace (not (and (.. game -input -activePointer -isDown)
                                                (> (.. game -input -activePointer -x)
                                                   (+ (/ width 2)
                                                      (/ width 4)))))]
                          (= isActive inPlace)))

    (upInputIsActive [this duration]
                     (let [x (.. game -input -activePointer -x)
                           width (.-width game)
                           isActive (.downDuration (.. this -input -keyboard) js/Phaser.Keyboard.SPACEBAR duration)
                           inPlace (not (and
                                         (.justPressed (.. game -input -activePointer) (+ duration (/ 1000 60)))
                                         (> x (/ width 4))
                                         (< x
                                            (+ (/ width 2)
                                               (/ width 4)))))]
                       (= isActive inPlace)))))

(defonce game (js/Phaser.Game. 480 320 js/Phaser.AUTO "app" nil false false))

(defn main []
  (.add (.-state game) "game" GameState true))
