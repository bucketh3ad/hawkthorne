(ns hawkthorne.config)

(def step 10000)
(def friction (* 0.146875 step))
(def max_x_speed 600)
(def max_y_speed 600)
(def jump_speed -670)
(def gravity (* 0.21875 step))
(def fall_grace 0.075)
(def fall_dps 15)
(def acceleration (* 0.046875 step))
(def deceleration (* 0.5 step))
(def airaccel (* 0.09375 step))
(def airdrag (* 0.96875 step))
