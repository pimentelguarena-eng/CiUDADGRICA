package com.graphics;

import java.util.ArrayList;
import java.util.List;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glScissor;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL33.*;

/**
 * CLASE ALUMNO: PROYECTO CIUDAD Y CONDUCCIÓN 11x11 COMPLETO.
 * 
 * INCLUYE REQUISITOS OBLIGATORIOS Y MEJORAS OPCIONALES (SECCIÓN 5):
 * 
 * 1. TRÁFICO AUTÓNOMO (4 VEHÍCULOS ADICIONALES):
 *    - Vehículos autónomos (Azul, Verde, Amarillo, Naranja) circulan sin intervención del usuario.
 *    - Siguen rutas en las avenidas del mapa 11x11 realizando giros en las intersecciones.
 *    - Mantienen posición, orientación y velocidad constante sin colisionar con edificios ni salir del mapa.
 *    - Se representan también en el minimapa.
 * 
 * 2. INTERFAZ Y PRESENTACIÓN URBANA:
 *    - Menú de Pausa (Tecla P) y Menú de Ayuda de Controles (Tecla H).
 *    - Señalización urbana y nombres de sectores ("SECTOR NORTE", "PARQUE CENTRAL", "SECTOR SUR").
 *    - Mobiliario urbano adicional: Paradas de autobús con marquesinas y vallas en las avenidas.
 */
public class claseAlumno extends clase4 {

    // ==================== 1. MATRIZ DEL MAPA (11x11) Y DESTINOS ====================

    protected static final int[][] MAPA_ALUMNO = {
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 0 (Norte): Calle continua
        {0, 1, 0, 1, 0, 2, 0, 1, 0, 1, 0}, // Fila 1: 4 Edificios, 1 Parque
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 2: Avenida 1
        {0, 2, 0, 1, 0, 1, 0, 2, 0, 1, 0}, // Fila 3: 3 Edificios, 2 Parques
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 4: Avenida 2
        {0, 1, 0, 2, 0, 1, 0, 1, 0, 1, 0}, // Fila 5: 4 Edificios, 1 Parque (Central)
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 6: Avenida 3
        {0, 1, 0, 1, 0, 2, 0, 1, 0, 2, 0}, // Fila 7: 3 Edificios, 2 Parques
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 8: Avenida 4
        {0, 1, 0, 2, 0, 1, 0, 1, 0, 1, 0}, // Fila 9: 4 Edificios, 1 Parque
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}  // Fila 10 (Sur): Calle continua
    };

    private static final float[][] DESTINOS_ALUMNO = {
        { 50, -50}, // Parada 1: Esquina Noreste
        {-50, -50}, // Parada 2: Esquina Noroeste
        {-50,  50}, // Parada 3: Esquina Sudoeste
        { 50,  50}  // Parada 4: Esquina Sudeste
    };

    private int entregasAlumno = 0;
    private float tiempoAlumno = 0;
    private boolean mostrarMapaAlumno = true;
    protected boolean nocheAlumno = true;
    protected boolean farosAlumno = true;

    // MEJORA SECCIÓN 5: Menú Principal de Inicio, Pausa (P) y Ayuda (H)
    private boolean menuInicio = true;
    private boolean enPausa = false;
    private boolean mostrarAyuda = false;

    // ==================== REMOLQUE / CHATA (UTILITY TRAILER) ====================
    private float chataX = -50;
    private float chataZ = 52.3f;
    private float chataAngulo = 0;
    private static final float LARGO_LANZA = 2.3f;

    protected static final float[][] LUCES_ALUMNO = {
        {-44, 4.5f, -44}, {-4, 4.5f, -44}, {36, 4.5f, -44},
        {-44, 4.5f,  -4}, {-4, 4.5f,  -4}, {36, 4.5f,  -4},
        {-44, 4.5f,  36}, {-4, 4.5f,  36}, {36, 4.5f,  36}
    };

    // ==================== 2. MEJORA OPCIONAL: TRÁFICO AUTÓNOMO ====================

    protected static class VehiculoAutonomo {
        float x, z;
        float angulo;
        float velocidad;
        float r, g, b;
        int waypointActual;
        float[][] waypoints;

        public VehiculoAutonomo(float[][] waypoints, float velocidad, float r, float g, float b) {
            this.waypoints = waypoints;
            this.x = waypoints[0][0];
            this.z = waypoints[0][1];
            this.velocidad = velocidad;
            this.r = r;
            this.g = g;
            this.b = b;
            this.waypointActual = 1;
            this.angulo = 0;
        }

        public void actualizar(float deltaTime, claseAlumno juego) {
            float targetX = waypoints[waypointActual][0];
            float targetZ = waypoints[waypointActual][1];
            float dx = targetX - x;
            float dz = targetZ - z;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);

            if (dist < 2.0f) {
                waypointActual = (waypointActual + 1) % waypoints.length;
                targetX = waypoints[waypointActual][0];
                targetZ = waypoints[waypointActual][1];
                dx = targetX - x;
                dz = targetZ - z;
            }

            float anguloDeseado = (float) Math.atan2(-dx, -dz);
            this.angulo = anguloDeseado;

            float frenteX = -(float) Math.sin(angulo);
            float frenteZ = -(float) Math.cos(angulo);
            float sigX = this.x + frenteX * velocidad * deltaTime;
            float sigZ = this.z + frenteZ * velocidad * deltaTime;

            // REQUISITO: Verificar colisión con manzanas antes de mover el vehículo autónomo
            if (juego.puedeCircular(sigX, sigZ)) {
                this.x = sigX;
                this.z = sigZ;
            } else {
                // Si encuentra un obstáculo, gira inmediatamente hacia el siguiente punto de la calle
                waypointActual = (waypointActual + 1) % waypoints.length;
            }
        }
    }

    private final List<VehiculoAutonomo> traficoAutonomo = new ArrayList<>();

    public claseAlumno() {
        super();
        inicializarTraficoAutonomo();
    }

    private void inicializarTraficoAutonomo() {
        traficoAutonomo.clear();
        // REQUISITO: Rutas alineadas a los centros exactos de las avenidas (-50, -30, -10, 10, 30, 50)
        
        // Auto 1 (Azul): Recorre el circuito de avenidas exteriores (Fila/Col 0 y 10: -50 y 50)
        float[][] ruta1 = {{-50, 50}, {50, 50}, {50, -50}, {-50, -50}};
        traficoAutonomo.add(new VehiculoAutonomo(ruta1, 9.5f, 0.15f, 0.45f, 0.95f));

        // Auto 2 (Verde): Recorre las avenidas horizontales intermedias (Fila 4: Z=-10 y Fila 6: Z=10)
        float[][] ruta2 = {{-50, -10}, {50, -10}, {50, 10}, {-50, 10}};
        traficoAutonomo.add(new VehiculoAutonomo(ruta2, 8.0f, 0.18f, 0.75f, 0.28f));

        // Auto 3 (Amarillo): Recorre las avenidas verticales intermedias (Col 4: X=-10 y Col 6: X=10)
        float[][] ruta3 = {{-10, -50}, {-10, 50}, {10, 50}, {10, -50}};
        traficoAutonomo.add(new VehiculoAutonomo(ruta3, 10.5f, 0.95f, 0.82f, 0.15f));

        // Auto 4 (Naranja): Recorre las avenidas intermedias internas (X,Z = -30 y 30)
        float[][] ruta4 = {{-30, -30}, {30, -30}, {30, 30}, {-30, 30}};
        traficoAutonomo.add(new VehiculoAutonomo(ruta4, 9.0f, 0.95f, 0.45f, 0.15f));
    }

    // ==================== 3. CONTROLES Y TECLADO ====================

    @Override
    protected void tecla(int key) {
        if (menuInicio) {
            if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER || key == GLFW_KEY_SPACE || key == GLFW_KEY_J) {
                menuInicio = false; // INICIA EL JUEGO Y ACTIVA TODO AL PRESIONAR EL BOTÓN JUGAR (ENTER/ESPACIO)
            }
            if (key == GLFW_KEY_H) {
                mostrarAyuda = !mostrarAyuda;
            }
            if (key == GLFW_KEY_R) {
                reiniciar();
            }
            actualizarTitulo();
            return;
        }

        super.tecla(key);

        if (key == GLFW_KEY_N) {
            nocheAlumno = !nocheAlumno;
            noche = nocheAlumno;
        }

        if (key == GLFW_KEY_F) {
            farosAlumno = !farosAlumno;
            faros = farosAlumno;
        }

        if (key == GLFW_KEY_M) {
            mostrarMapaAlumno = !mostrarMapaAlumno;
        }

        // MEJORA SECCIÓN 5: Tecla P para Menú de Pausa
        if (key == GLFW_KEY_P) {
            enPausa = !enPausa;
        }

        // MEJORA SECCIÓN 5: Tecla H para Menú de Ayuda
        if (key == GLFW_KEY_H) {
            mostrarAyuda = !mostrarAyuda;
        }

        if (key == GLFW_KEY_R) {
            reiniciar();
        }
    }

    @Override
    protected void reiniciar() {
        super.reiniciar();
        entregasAlumno = 0;
        tiempoAlumno = 0;
        autoX = -50;
        autoZ = 50;
        angulo = 0;
        velocidad = 0;
        chataX = -50;
        chataZ = 50 + LARGO_LANZA;
        chataAngulo = 0;
        enPausa = false;
        menuInicio = true; // AL REINICIAR CON R VUELVE AL MENÚ DE INICIO INICIAL
        inicializarTraficoAutonomo();
    }

    // ==================== 4. ACTUALIZACIÓN POR CUADRO ====================

    @Override
    protected void actualizar(float deltaTime) {
        if (menuInicio) {
            actualizarTitulo();
            return; // Mientras esté activo el Menú de Inicio, no avanza el tiempo ni la física
        }

        if (enPausa) {
            actualizarTitulo();
            return; // En pausa se suspende la física y movimiento
        }

        super.actualizar(deltaTime);

        // Actualizar cinemática articulada de la chata (utility trailer)
        float atrasX = (float) Math.sin(angulo);
        float atrasZ = (float) Math.cos(angulo);
        float engancheX = autoX + atrasX * 1.45f;
        float engancheZ = autoZ + atrasZ * 1.45f;

        float dxChata = engancheX - chataX;
        float dzChata = engancheZ - chataZ;
        float distChata = (float) Math.sqrt(dxChata * dxChata + dzChata * dzChata);

        if (distChata > 0.001f) {
            if (velocidad >= 0) {
                // Hacia adelante: la chata se orienta hacia el punto de enganche
                chataAngulo = (float) Math.atan2(-dxChata, -dzChata);
            } else {
                // Reversa: cinemática de remolque empujado
                float difAngulo = angulo - chataAngulo;
                while (difAngulo > Math.PI) difAngulo -= 2 * Math.PI;
                while (difAngulo < -Math.PI) difAngulo += 2 * Math.PI;
                chataAngulo -= difAngulo * Math.abs(velocidad) * 0.15f * deltaTime;
            }
            chataX = engancheX + (float) Math.sin(chataAngulo) * LARGO_LANZA;
            chataZ = engancheZ + (float) Math.cos(chataAngulo) * LARGO_LANZA;
        }

        // Actualizar movimiento de los vehículos autónomos validando colisiones
        for (VehiculoAutonomo v : traficoAutonomo) {
            v.actualizar(deltaTime, this);
        }

        if (entregasAlumno >= DESTINOS_ALUMNO.length) {
            return;
        }

        tiempoAlumno += deltaTime;

        float destinoX = DESTINOS_ALUMNO[entregasAlumno][0];
        float destinoZ = DESTINOS_ALUMNO[entregasAlumno][1];
        float dx = autoX - destinoX;
        float dz = autoZ - destinoZ;

        if (dx * dx + dz * dz < 3.5f * 3.5f && Math.abs(velocidad) < 1.2f) {
            entregasAlumno++;
        }
    }

    @Override
    protected String estadoExtra() {
        if (menuInicio) {
            return " | MENÚ DE INICIO | Presiona ENTER o ESPACIO para JUGAR";
        }
        String estadoPausa = enPausa ? " [PAUSA ACTIVA - P: Reanudar]" : "";
        String estadoAyuda = mostrarAyuda ? " [AYUDA CONTROLES: WASD/Flechas=Conducir | Espacio=Freno | N=Día/Noche | F=Faros | M=Minimapa | P=Pausa | H=Ocultar | R=Reiniciar]" : "";
        String mensaje = " | N: noche | F: faros | M: mapa | P: pausa | H: ayuda" + estadoPausa + estadoAyuda + " | ";
        if (entregasAlumno == DESTINOS_ALUMNO.length) {
            mensaje += "¡GANASTE! 4/4 entregas en " + (int) tiempoAlumno + " s! R: Reiniciar";
        } else {
            mensaje += "Entregas " + entregasAlumno + "/4 | " + (int) tiempoAlumno + " s";
        }
        return mensaje;
    }



    // ==================== 5. PREPARACIÓN DE LUCES ====================

    @Override
    protected void prepararLuces() {
        entero("uNoche", nocheAlumno ? 1 : 0);
        entero("uFaros", farosAlumno ? 1 : 0);
        entero("uEmision", 0);

        for (int i = 0; i < LUCES_ALUMNO.length; i++) {
            vector("uLuces[" + i + "]", LUCES_ALUMNO[i][0], LUCES_ALUMNO[i][1], LUCES_ALUMNO[i][2]);
        }

        float frenteX = -(float) Math.sin(angulo);
        float frenteZ = -(float) Math.cos(angulo);
        vector("uAuto", autoX, 0.7f, autoZ);
        vector("uFrente", frenteX, 0, frenteZ);
    }

    // ==================== 6. ESCENA Y PRESENTACIÓN URBANA ====================

    @Override
    protected void escena() {
        // Suelo 110x110
        caja(0, -0.25f, 0, 110, 0.5f, 110, 0.16f, 0.19f, 0.23f);

        // Manzanas
        for (int fila = 0; fila < MAPA_ALUMNO.length; fila++) {
            for (int columna = 0; columna < MAPA_ALUMNO[fila].length; columna++) {
                float x = centro(columna);
                float z = centro(fila);
                int tipo = MAPA_ALUMNO[fila][columna];

                if (tipo == 0) {
                    dibujarMarcasCalle(fila, columna, x, z);
                } else {
                    caja(x, 0.15f, z, 10, 0.3f, 10, 0.60f, 0.64f, 0.66f); // Acera

                    if (tipo == 1) {
                        float altura = 6 + (fila * 4 + columna * 5) % 11;
                        float rojo = 0.25f + columna * 0.05f;
                        float azul = 0.40f + fila * 0.04f;
                        caja(x, altura / 2 + 0.3f, z, 7, altura, 7, rojo, 0.42f, azul);
                        caja(x, altura + 0.45f, z, 7.3f, 0.3f, 7.3f, 0.20f, 0.26f, 0.32f);

                        if (!vistaMapa) {
                            dibujarVentanasAlumno(x, z, altura);
                        }
                    } else { // Parque
                        caja(x, 0.32f, z, 9, 0.1f, 9, 0.20f, 0.48f, 0.26f);
                        if (!vistaMapa) {
                            dibujarParqueAlumno(x, z);
                        }
                    }

                    if (tipo != 0 && !vistaMapa) {
                        dibujarPasoPeatonalAlumno(x, z);
                        dibujarSemaforoAlumno(x + 4, z - 4);
                    }
                }
            }
        }

        // MEJORA SECCIÓN 5: Mobiliario Urbano
        if (!vistaMapa) {
            // dibujarCartelesSectores(); // Eliminados los carteles/líneas azul y verde de sectores
            dibujarParadasAutobus();
        }

        // Renderizado de Farolas
        for (float[] luz : LUCES_ALUMNO) {
            caja(luz[0], 2.2f, luz[2], 0.18f, 4.4f, 0.18f, 0.20f, 0.24f, 0.28f);
            if (nocheAlumno) entero("uEmision", 1);
            caja(luz[0], luz[1], luz[2], 0.7f, 0.35f, 0.7f, 1, 0.83f, 0.42f);
            entero("uEmision", 0);
        }

        // Renderizado del auto del jugador y su chata utilitaria
        dibujarAuto();
        dibujarChata();

        // MEJORA SECCIÓN 5: Renderizado del Tráfico Autónomo (4 Vehículos)
        for (VehiculoAutonomo v : traficoAutonomo) {
            dibujarVehiculoAutonomo(v);
        }

        // Destino activo
        if (entregasAlumno < DESTINOS_ALUMNO.length) {
            dibujarDestinoAlumno();
        }
    }

    // MEJORA SECCIÓN 5: Dibujo de Vehículo Autónomo
    private void dibujarVehiculoAutonomo(VehiculoAutonomo v) {
        float coseno = (float) Math.cos(v.angulo);
        float seno = (float) Math.sin(v.angulo);

        // Carrocería
        cajaGirada(v.x, 0.65f, v.z, 1.6f, 0.55f, 2.5f, v.r, v.g, v.b, v.angulo);
        // Cabina oscura
        cajaGirada(v.x, 1.12f, v.z + 0.1f, 1.25f, 0.5f, 1.2f, 0.15f, 0.18f, 0.22f, v.angulo);

        // Ruedas
        float[] lados = {-0.85f, 0.85f};
        float[] ejes = {-0.8f, 0.8f};
        for (float rx : lados) {
            for (float rz : ejes) {
                float mx = v.x + coseno * rx + seno * rz;
                float mz = v.z - seno * rx + coseno * rz;
                cajaGirada(mx, 0.38f, mz, 0.22f, 0.55f, 0.55f, 0.05f, 0.05f, 0.06f, v.angulo);
            }
        }

        // Faros encendidos de noche
        if (nocheAlumno) {
            entero("uEmision", 1);
            float[] farosLados = {-0.5f, 0.5f};
            for (float fx : farosLados) {
                float mx = v.x + coseno * fx - seno * 1.3f;
                float mz = v.z - seno * fx - coseno * 1.3f;
                cajaGirada(mx, 0.68f, mz, 0.35f, 0.18f, 0.06f, 1.0f, 0.95f, 0.6f, v.angulo);
            }
            entero("uEmision", 0);
        }
    }

    // ==================== MODELADO 3D DE LA CHATA (UTILITY TRAILER) ====================

    /** Transforma y dibuja una pieza en el espacio local del remolque utilitario (chata). */
    private void piezaChata(float lx, float ly, float lz, float sx, float sy, float sz, float r, float g, float b) {
        float cos = (float) Math.cos(chataAngulo);
        float sen = (float) Math.sin(chataAngulo);
        float mx = chataX + cos * lx + sen * lz;
        float mz = chataZ - sen * lx + cos * lz;
        cajaGirada(mx, ly, mz, sx, sy, sz, r, g, b, chataAngulo);
    }

   
    private void dibujarChata() {
       

        // Plataforma
        piezaChata(0, 0.48f, 0, 1.70f, 0.12f, 2.20f, 0.52f, 0.38f, 0.22f);

        // Chasis estructural metálico inferior
        piezaChata(0, 0.38f, 0, 1.62f, 0.10f, 2.22f, 0.22f, 0.24f, 0.28f);

        // Baranda frontal
        piezaChata(0, 0.72f, -1.05f, 1.70f, 0.40f, 0.10f, 0.28f, 0.32f, 0.36f);
        // Baranda trasera / compuerta de carga
        piezaChata(0, 0.72f, 1.05f, 1.70f, 0.40f, 0.10f, 0.28f, 0.32f, 0.36f);
        // Barandas laterales izquierda y derecha
        piezaChata(-0.80f, 0.72f, 0, 0.10f, 0.40f, 2.20f, 0.28f, 0.32f, 0.36f);
        piezaChata(0.80f, 0.72f, 0, 0.10f, 0.40f, 2.20f, 0.28f, 0.32f, 0.36f);

        // Pasamanos / perfiles superiores de las barandas
        piezaChata(0, 0.94f, -1.05f, 1.74f, 0.08f, 0.12f, 0.85f, 0.85f, 0.90f);
        piezaChata(-0.80f, 0.94f, 0, 0.12f, 0.08f, 2.24f, 0.85f, 0.85f, 0.90f);
        piezaChata(0.80f, 0.94f, 0, 0.12f, 0.08f, 2.24f, 0.85f, 0.85f, 0.90f);

        //Eje, Guardabarros y Ruedas a ambos lados
        float[] ladosRuedas = {-0.98f, 0.98f};
        for (float lx : ladosRuedas) {
            // Guardabarros
            piezaChata(lx * 0.96f, 0.70f, 0, 0.26f, 0.08f, 0.78f, 0.24f, 0.26f, 0.30f);
            // Neumático
            piezaChata(lx, 0.38f, 0, 0.22f, 0.58f, 0.58f, 0.05f, 0.05f, 0.06f);
            // Llanta metálica central
            piezaChata(lx * 1.01f, 0.38f, 0, 0.22f, 0.26f, 0.26f, 0.75f, 0.78f, 0.82f);
        }

        
       //Luces traseras rojas / reflectores
        if (nocheAlumno) entero("uEmision", 1);
        piezaChata(-0.65f, 0.55f, 1.12f, 0.25f, 0.14f, 0.06f, 0.90f, 0.10f, 0.10f);
        piezaChata(0.65f, 0.55f, 1.12f, 0.25f, 0.14f, 0.06f, 0.90f, 0.10f, 0.10f);
        entero("uEmision", 0);
    }

    // MEJORA SECCIÓN 5: Carteles y Letreros de Sectores de la Ciudad
    private void dibujarCartelesSectores() {
        entero("uEmision", 1);
        // Sector Norte (Azul brillante)
        caja(0, 6.5f, -50, 12, 1.2f, 0.4f, 0.15f, 0.45f, 0.95f);
        // Sector Sur (Verde brillante)
        caja(0, 6.5f, 50, 12, 1.2f, 0.4f, 0.18f, 0.80f, 0.35f);
        // Parque Central (Dorado)
        caja(0, 5.5f, 0, 10, 1.0f, 0.4f, 0.95f, 0.75f, 0.15f);
        entero("uEmision", 0);
    }

    // MEJORA SECCIÓN 5: Paradas de Autobús con marquesina y asientos
    private void dibujarParadasAutobus() {
        float[][] paradas = {{-20, -10}, {20, 10}};
        for (float[] p : paradas) {
            caja(p[0], 1.8f, p[1], 0.15f, 3.6f, 0.15f, 0.2f, 0.2f, 0.2f); // Soporte
            caja(p[0], 3.6f, p[1], 3.5f, 0.15f, 2.0f, 0.4f, 0.6f, 0.8f);   // Marquesina
            caja(p[0], 0.6f, p[1], 2.8f, 0.2f, 0.7f, 0.5f, 0.3f, 0.1f);   // Asiento
        }
    }

    private void dibujarDestinoAlumno() {
        float x = DESTINOS_ALUMNO[entregasAlumno][0];
        float z = DESTINOS_ALUMNO[entregasAlumno][1];
        entero("uEmision", 1);
        caja(x, 0.06f, z, 5, 0.08f, 5, 1, 0.72f, 0.12f);
        if (!vistaMapa) {
            float altura = 3.5f + (float) Math.sin(tiempoAlumno * 2) * 0.3f;
            cajaGirada(x, altura, z, 0.8f, 0.8f, 0.8f, 1, 0.8f, 0.15f, tiempoAlumno);
        }
        entero("uEmision", 0);
    }

    private void dibujarMarcasCalle(int fila, int columna, float x, float z) {
        if (fila % 2 == 0 && columna % 2 == 1) {
            for (int d = -3; d <= 3; d += 3) {
                caja(x + d, 0.025f, z, 1.6f, 0.03f, 0.13f, 1, 0.84f, 0.35f);
            }
        }
        if (columna % 2 == 0 && fila % 2 == 1) {
            for (int d = -3; d <= 3; d += 3) {
                caja(x, 0.025f, z + d, 0.13f, 0.03f, 1.6f, 1, 0.84f, 0.35f);
            }
        }
    }

    private void dibujarParqueAlumno(float x, float z) {
        float[] pos = {-2.5f, 2.5f};
        for (float dx : pos) {
            for (float dz : pos) {
                caja(x + dx, 1.2f, z + dz, 0.35f, 2f, 0.35f, 0.38f, 0.22f, 0.12f);
                caja(x + dx, 2.7f, z + dz, 2f, 2.3f, 2f, 0.12f, 0.42f, 0.23f);
            }
        }
        caja(x, 0.65f, z, 3f, 0.25f, 0.8f, 0.55f, 0.30f, 0.13f);
        caja(x, 0.4f, z, 2f, 0.6f, 0.35f, 0.22f, 0.24f, 0.24f);
        caja(x, 1f, z + 0.35f, 3f, 0.7f, 0.15f, 0.55f, 0.30f, 0.13f);
    }

    private void dibujarVentanasAlumno(float x, float z, float altura) {
        if (nocheAlumno) entero("uEmision", 1);
        for (float y = 1.7f; y < altura; y += 2) {
            for (float d = -2; d <= 2; d += 2) {
                caja(x + d, y, z - 3.51f, 0.8f, 0.9f, 0.04f, 0.95f, 0.75f, 0.38f);
                caja(x + d, y, z + 3.51f, 0.8f, 0.9f, 0.04f, 0.95f, 0.75f, 0.38f);
                caja(x - 3.51f, y, z + d, 0.04f, 0.9f, 0.8f, 0.95f, 0.75f, 0.38f);
                caja(x + 3.51f, y, z + d, 0.04f, 0.9f, 0.8f, 0.95f, 0.75f, 0.38f);
            }
        }
        entero("uEmision", 0);
    }

    private void dibujarPasoPeatonalAlumno(float x, float z) {
        for (int d = -3; d <= 3; d++) {
            caja(x + d, 0.045f, z - 8f, 0.45f, 0.04f, 2f, 0.85f, 0.87f, 0.83f);
        }
    }
    
    private void dibujarSemaforoAlumno(float x, float z) {
        caja(x, 1.7f, z, 0.18f, 2.8f, 0.18f, 0.18f, 0.20f, 0.22f);
        caja(x, 3.1f, z, 0.55f, 1.2f, 0.45f, 0.08f, 0.10f, 0.12f);
        int fase = (int) (tiempoAlumno % 12);
        for (int i = 0; i < 3; i++) {
            boolean enc = (i == 0 && fase < 5) || (i == 1 && fase >= 10) || (i == 2 && fase >= 5 && fase < 10);
            float brillo = enc ? 1f : 0.15f;
            entero("uEmision", enc ? 1 : 0);
            float r = i < 2 ? brillo : 0;
            float g = i > 0 ? brillo : 0;
            caja(x, 3.45f - i * 0.35f, z - 0.24f, 0.28f, 0.25f, 0.06f, r, g, 0.02f);
        }
        entero("uEmision", 0);
    }

    // ==================== 7. MINIMAPA Y PANELES HUD / PAUSA / AYUDA / MENÚ INICIO ====================

    @Override
    protected void dibujarFrame() {
        super.dibujarFrame();

        // MEJORA SECCIÓN 5: Menú Principal de Inicio antes de empezar el juego
        if (menuInicio) {
            dibujarMenuInicio();
            return;
        }

        // MEJORA SECCIÓN 5: Panel HUD dentro de la ventana con Velocidad y Estado de Luces
        dibujarPanelHUD();

        // MEJORA SECCIÓN 5: Menú / Panel de Pausa (P) o Ayuda de Controles (H)
        if (enPausa || mostrarAyuda) {
            dibujarPanelPausaAyuda();
        }

        if (!mostrarMapaAlumno) {
            return;
        }

        int dimensionMenor = Math.min(ancho, alto);
        int lado = Math.min(260, dimensionMenor / 3);
        int margen = Math.min(18, dimensionMenor / 20);
        int x = ancho - lado - margen;
        int y = alto - lado - margen;

        glEnable(GL_SCISSOR_TEST);
        glScissor(x - 3, y - 3, lado + 6, lado + 6);
        glClearColor(0.8f, 0.87f, 0.94f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glScissor(x, y, lado, lado);
        glClearColor(0.06f, 0.10f, 0.15f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glViewport(x, y, lado, lado);
        entero("uMapa", 1);
        vistaMapa = true;

        try {
            escena();
            dibujarIndicadorAutoAlumno();
            // Renderizar vehículos autónomos en el minimapa
            for (VehiculoAutonomo v : traficoAutonomo) {
                cajaGirada(v.x, 24f, v.z, 2.0f, 0.1f, 2.8f, v.r, v.g, v.b, v.angulo);
            }
        } finally {
            vistaMapa = false;
            entero("uMapa", 0);
            glDisable(GL_SCISSOR_TEST);
            glViewport(0, 0, ancho, alto);
        }
    }

    /** MEJORA SECCIÓN 5: Panel HUD dentro de la ventana con velocímetro dinámico y textos legibles. */
    private void dibujarPanelHUD() {
        glViewport(0, 0, ancho, alto);
        entero("uMapa", 1);
        vistaMapa = true;

        try {
            // Fondo Panel HUD (Esquina inferior izquierda)
            caja(-34, 10f, 32, 30, 1.2f, 20, 0.70f, 0.78f, 0.88f);
            caja(-34, 12f, 32, 28, 1.0f, 18, 0.08f, 0.12f, 0.18f);

            int kmh = Math.round(Math.abs(velocidad) * 3.6f);
            dibujarTexto("VELOCIDAD: " + kmh + " KM/H", -47, 26, 0.85f, 1.0f, 1.0f, 0.2f);

            // Fondo barra de velocidad (velocímetro)
            caja(-34, 14f, 31, 22, 0.8f, 2.5f, 0.2f, 0.22f, 0.25f);
            float fracVel = Math.min(1.0f, Math.abs(velocidad) / 16.0f);
            float rVel = fracVel;
            float gVel = 1.0f - fracVel * 0.5f;
            if (fracVel > 0.05f) {
                float anchoBarra = 21.0f * fracVel;
                caja(-44.5f + anchoBarra / 2.0f, 15f, 31, anchoBarra, 0.9f, 2.2f, rVel, gVel, 0.15f);
            }

            // Indicador Faros (F)
            if (farosAlumno) {
                dibujarTexto("F:FAROS ON", -47, 37, 0.8f, 1.0f, 0.9f, 0.2f);
            } else {
                dibujarTexto("F:FAROS OFF", -47, 37, 0.8f, 0.5f, 0.5f, 0.5f);
            }

            // Indicador Noche (N)
            if (nocheAlumno) {
                dibujarTexto("N:NOCHE", -32, 37, 0.8f, 0.3f, 0.6f, 1.0f);
            } else {
                dibujarTexto("N:DIA", -32, 37, 0.8f, 0.95f, 0.75f, 0.2f);
            }
        } finally {
            vistaMapa = false;
            entero("uMapa", 0);
        }
    }

    /** MEJORA SECCIÓN 5: Menú / Overlay dentro de la ventana para Pausa (P) y Ayuda (H) con texto legible. */
    private void dibujarPanelPausaAyuda() {
        glViewport(0, 0, ancho, alto);
        entero("uMapa", 1);
        vistaMapa = true;

        try {
            // Fondo Marco
            caja(0, 13f, 0, 80, 1.2f, 52, enPausa ? 0.95f : 0.20f, enPausa ? 0.25f : 0.75f, enPausa ? 0.20f : 0.95f);
            caja(0, 15f, 0, 76, 1.0f, 48, 0.06f, 0.09f, 0.16f);

            if (enPausa) {
                dibujarTexto("JUEGO EN PAUSA", -22, -12, 1.5f, 1.0f, 0.2f, 0.2f);
                dibujarTexto("P: REANUDAR", -14, 0, 1.3f, 1.0f, 0.9f, 0.3f);
            }

            if (mostrarAyuda) {
                float startZ = enPausa ? -2f : -18f;
                dibujarTexto("AYUDA DE CONTROLES", -26, startZ, 1.4f, 0.2f, 0.85f, 1.0f);
                dibujarTexto("WASD/FLECHAS: CONDUCIR", -32, startZ + 8f, 1.0f, 0.9f, 0.9f, 0.9f);
                dibujarTexto("ESPACIO: FRENO DE MANO", -32, startZ + 15f, 1.0f, 1.0f, 0.4f, 0.4f);
                dibujarTexto("N: NOCHE   F: FAROS", -32, startZ + 22f, 1.0f, 1.0f, 0.85f, 0.2f);
                dibujarTexto("M: MAPA   P: PAUSA   R: RESET", -32, startZ + 29f, 0.95f, 0.4f, 0.9f, 0.4f);
            }
        } finally {
            vistaMapa = false;
            entero("uMapa", 0);
        }
    }

    /** MEJORA SECCIÓN 5: Submenú Principal de Inicio antes de empezar el juego. */
    private void dibujarMenuInicio() {
        glViewport(0, 0, ancho, alto);
        entero("uMapa", 1);
        vistaMapa = true;

        try {
            // Fondo Marco Menú de Inicio
            caja(0, 10f, 0, 84, 1.5f, 56, 0.20f, 0.75f, 0.95f);
            caja(0, 12f, 0, 80, 1.2f, 52, 0.04f, 0.07f, 0.14f);

            // Título Principal y Subtítulo
            dibujarTexto("CIUDAD Y CONDUCCION 3D", -34, -18f, 1.6f, 1.0f, 0.85f, 0.15f);
            dibujarTexto("PROYECTO PROGRAMACION GRAFICA", -33, -10f, 1.15f, 0.3f, 0.8f, 1.0f);

            // Botón JUGAR destacadutility trailer o
            caja(0, 14f, 2f, 65, 1.0f, 9, 0.15f, 0.65f, 0.25f);
            dibujarTexto("PRESIONA ENTER PARA JUGAR", -30, 2f, 1.25f, 1.0f, 1.0f, 1.0f);

            // Botón / Instrucción Ayuda H
            caja(0, 14f, 13f, 65, 1.0f, 7.5f, 0.20f, 0.35f, 0.55f);
            dibujarTexto("PRESIONA H PARA VER CONTROLES", -29, 13f, 1.1f, 1.0f, 0.9f, 0.3f);

            // Texto Informativo Objetivo
            dibujarTexto("OBJETIVO: COMPLETA LAS 4 ENTREGAS EN LA CIUDAD", -34, 21f, 0.90f, 0.85f, 0.9f, 1.0f);

            if (mostrarAyuda) {
                dibujarTexto("CONTROLES: WASD=AUTO  ESPACIO=FRENO  N=NOCHE  F=FAROS  M=MAPA  P=PAUSA  R=RESET", -36, 24f, 0.65f, 0.95f, 0.4f, 0.2f);
            }
        } finally {
            vistaMapa = false;
            entero("uMapa", 0);
        }
    }

    // ==================== 8. SISTEMA DE TEXTO VECTORIAL 2D PARA PANELES ====================

    private void dibujarLetra(char c, float cx, float cy, float s, float r, float g, float b) {
        c = Character.toUpperCase(c);
        float w = 1.2f * s;
        float h = 1.8f * s;
        float t = 0.38f * s;
        float cz = cy;
        float y = 18f;

        boolean top = "A B C D E F G H I K L O P Q R S T Z 0 2 3 5 6 7 8 9 ?".indexOf(c) >= 0;
        boolean mid = "A B E F G H P Q R S Y 2 3 4 5 6 8 9 - = ?".indexOf(c) >= 0;
        boolean bot = "B C D E G J L O Q S Z 0 2 3 5 6 8 9 _ =".indexOf(c) >= 0;
        boolean ul  = "A C E F G H K M N O P Q R S U V W X Y 0 4 5 6 8 9".indexOf(c) >= 0;
        boolean ur  = "A B D H K M N O P Q R U V W X Y 0 1 2 3 4 7 8 9 ?".indexOf(c) >= 0;
        boolean ll  = "A B C D E F G H K L M N O P Q R U V W 0 2 6 8".indexOf(c) >= 0;
        boolean lr  = "A B D G H M N O Q S U V W X Y 0 1 3 4 5 6 7 8 9".indexOf(c) >= 0;

        entero("uEmision", 1);

        if (top) caja(cx, y, cz - h, w, t, t, r, g, b);
        if (mid) caja(cx, y, cz, w, t, t, r, g, b);
        if (bot) caja(cx, y, cz + h, w, t, t, r, g, b);
        if (ul)  caja(cx - w / 2, y, cz - h / 2, t, t, h, r, g, b);
        if (ur)  caja(cx + w / 2, y, cz - h / 2, t, t, h, r, g, b);
        if (ll)  caja(cx - w / 2, y, cz + h / 2, t, t, h, r, g, b);
        if (lr)  caja(cx + w / 2, y, cz + h / 2, t, t, h, r, g, b);

        if (c == 'I' || c == 'T') {
            caja(cx, y, cz, t, t, h * 2, r, g, b);
        }
        if (c == 'M' || c == 'W' || c == 'N') {
            caja(cx, y, cz, t, t, h, r, g, b);
        }
        if (c == ':') {
            caja(cx, y, cz - h / 2, t * 1.5f, t * 1.5f, t * 1.5f, r, g, b);
            caja(cx, y, cz + h / 2, t * 1.5f, t * 1.5f, t * 1.5f, r, g, b);
        }
        if (c == '/') {
            cajaGirada(cx, y, cz, t, t, h * 2.2f, r, g, b, 0.4f);
        }
        if (c == '!') {
            caja(cx, y, cz - h / 4, t * 1.5f, t * 1.5f, h * 1.2f, r, g, b);
            caja(cx, y, cz + h, t * 1.5f, t * 1.5f, t * 1.5f, r, g, b);
        }

        entero("uEmision", 0);
    }

    private void dibujarTexto(String str, float startX, float startZ, float scale, float r, float g, float b) {
        float spacing = 2.0f * scale;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c != ' ') {
                dibujarLetra(c, startX + i * spacing, startZ, scale, r, g, b);
            }
        }
    }



    private void dibujarIndicadorAutoAlumno() {
        cajaGirada(autoX, 25f, autoZ, 2.2f, 0.1f, 3.2f, 0.1f, 1f, 1f, angulo);
        float frenteX = -(float) Math.sin(angulo);
        float frenteZ = -(float) Math.cos(angulo);
        float puntaX = autoX + frenteX * 2f;
        float puntaZ = autoZ + frenteZ * 2f;
        caja(puntaX, 26f, puntaZ, 0.9f, 0.1f, 0.9f, 1f, 1f, 1f);

        // Indicador de la chata en el minimapa (marrón con borde brillante)
        cajaGirada(chataX, 24.5f, chataZ, 1.8f, 0.1f, 2.3f, 0.65f, 0.45f, 0.22f, chataAngulo);
    }

    // ==================== 8. COLISIONES ====================

    @Override
    protected boolean puedeCircular(float x, float z) {
        float limitePermitido = LIMITE - RADIO_AUTO;
        if (Math.abs(x) > limitePermitido || Math.abs(z) > limitePermitido) {
            return false;
        }

        for (int fila = 0; fila < MAPA_ALUMNO.length; fila++) {
            for (int columna = 0; columna < MAPA_ALUMNO[fila].length; columna++) {
                if (MAPA_ALUMNO[fila][columna] == 0) continue;

                float centroX = centro(columna);
                float centroZ = centro(fila);
                float cercaX = Math.max(centroX - 5, Math.min(x, centroX + 5));
                float cercaZ = Math.max(centroZ - 5, Math.min(z, centroZ + 5));
                float dx = x - cercaX;
                float dz = z - cercaZ;

                if (dx * dx + dz * dz < RADIO_AUTO * RADIO_AUTO) {
                    return false;
                }
            }
        }
        return true;
    }

    // ==================== 9. PUNTO DE ENTRADA ====================

    @Override
    public void run() {
        super.run();
    }

    public static void main(String[] args) {
        claseAlumno aplicacion = new claseAlumno();
        aplicacion.run();
    }
}
