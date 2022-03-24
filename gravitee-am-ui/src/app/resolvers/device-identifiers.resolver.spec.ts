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
import {inject, TestBed} from '@angular/core/testing';
import {FactorPluginsResolver} from './factor-plugins.resolver';
import {DeviceIdentifierPluginsResolver} from "./device-identifier-plugins.resolver";
import {DeviceIdentifiersResolver} from "./device-identifiers.resolver";

describe('DeviceIdentifiersResolver', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
    providers: [FactorPluginsResolver],
    teardown: { destroyAfterEach: false }
});
  });

  it('should ...', inject([DeviceIdentifiersResolver], (service: DeviceIdentifiersResolver) => {
    expect(service).toBeTruthy();
  }));
});
