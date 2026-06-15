import { ChangeDetectionStrategy, Component } from '@angular/core';

import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-settings',
  template: `
    <div class="page-header">
      <div>
        <span class="eyebrow">Sistema</span>
        <h1>Configuracion</h1>
        <p>Informacion tecnica del entorno y puntos de integracion.</p>
      </div>
    </div>

    <div class="settings-grid">
      <article class="panel">
        <span class="eyebrow">Entorno</span>
        <h2>Conexion con la API</h2>
        <dl class="settings-list">
          <div><dt>Modo</dt><dd>{{ production ? 'Produccion' : 'Desarrollo' }}</dd></div>
          <div><dt>API base</dt><dd class="mono">{{ apiUrl }}</dd></div>
          <div><dt>Frontend</dt><dd>Angular 21 standalone</dd></div>
        </dl>
      </article>
      <article class="panel">
        <span class="eyebrow">Seguridad</span>
        <h2>Configuracion protegida</h2>
        <p class="muted">
          Las URLs y credenciales productivas se suministran durante el build o desde el gestor
          de variables de la plataforma. No se almacenan secretos en el repositorio.
        </p>
      </article>
      <article class="panel">
        <span class="eyebrow">Servicios</span>
        <h2>Backend Spring Boot</h2>
        <ul class="check-list">
          <li>API REST versionada</li>
          <li>PostgreSQL y Flyway</li>
          <li>Health checks con Actuator</li>
          <li>CORS configurable por ambiente</li>
        </ul>
      </article>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Settings {
  protected readonly production = environment.production;
  protected readonly apiUrl = environment.apiUrl || 'Definida durante el build productivo';
}
