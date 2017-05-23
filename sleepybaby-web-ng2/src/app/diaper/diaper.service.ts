/*
 * Copyright 2017 Adam Jordens
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Injectable} from '@angular/core';
import {Headers, Http, Response} from '@angular/http';

import 'rxjs/add/operator/toPromise';

import {DiaperAggregate} from './diaper-aggregate';
import {ExceptionHandlerService} from "../exceptions/exception-handler.service";

@Injectable()
export class DiaperService {
  private diapersUrl = '/api/diapers/';

  constructor(private http: Http, private exception: ExceptionHandlerService) {
  }

  getDiapersByDay(): Promise<Array<DiaperAggregate>> {
    let headers = new Headers({
      'Content-Type': 'application/json'
    });

    let currentUser = localStorage.getItem('currentUser');
    if (currentUser) {
      headers.append('Authorization', JSON.parse(currentUser).token)
    }

    return this.http
      .get(this.diapersUrl + '/byDay', {'headers': headers})
      .toPromise()
      .then(res => res.json().result.diaperSummariesByDay)
      .catch(this.handleError.bind(this));
  }

  private handleError(error: any): Promise<any> {
    return this.exception.handleTransportError(error);
  }
}
