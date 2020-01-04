/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, RouterStateSnapshot} from '@angular/router';
import {concat, Observable, of} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {AuthService} from './../services/auth.service';
import {DomainService} from '../services/domain.service';
import {ApplicationService} from '../services/application.service';

@Injectable()
export class AuthGuard implements CanActivate {

  constructor(private authService: AuthService,
              private domainService: DomainService,
              private applicationService: ApplicationService) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
    // if no permission required, continue
    if (!route.data || !route.data.perms || !route.data.perms.only) {
      return true;
    }
    // check if we need to load some data
    const userResult: Observable<any> = !this.authService.user() ? this.authService.userInfo() : of(this.authService.user());
    let resourceResult: Observable<any> = of();
    if (!state.url.startsWith('/settings')) {
      // check if the authenticated user can navigate to the next route (domain settings or application settings)
      const domainId = route.parent.paramMap.get('domainId') ? route.parent.paramMap.get('domainId') : route.parent.parent.paramMap.get('domainId') ? route.parent.parent.paramMap.get('domainId') : route.parent.parent.parent.paramMap.get('domainId');
      const appId = route.parent.paramMap.get('appId') ? route.parent.paramMap.get('appId') : route.parent.parent.paramMap.get('appId');
      // if permissions have been already loaded, continue;
      if ((appId && !this.authService.applicationPermissionsLoaded()) || (domainId && !this.authService.domainPermissionsLoaded())) {
        resourceResult = (domainId && appId) ? this.applicationService.permissions(domainId, appId) : this.domainService.permissions(domainId);
      }
    }
    // check permissions
    const requiredPerms = route.data.perms.only;
    return concat(userResult, resourceResult).pipe(
      map(() =>  this.isAuthorized(requiredPerms)),
      catchError((err) => {
        return of(false);
      })
    );

  }

  canDisplay(route: ActivatedRouteSnapshot, path): boolean {
    // if no permission required, continue
    if (!path.data || !path.data.perms || !path.data.perms.only) {
      return true;
    }
    // check if the authenticated user can display UI items
    const requiredPerms = path.data.perms.only;
    return this.isAuthorized(requiredPerms);
  }

  private isAuthorized(requiredPerms): boolean {
    return this.authService.isAdmin() || this.authService.hasPermissions(requiredPerms);
  }
}