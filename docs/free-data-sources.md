# Free data strategy

Possible future adapters: [OpenStreetMap](https://www.openstreetmap.org/) with [Leaflet](https://leafletjs.com/) for maps; [Nominatim](https://operations.osmfoundation.org/policies/nominatim/) for light geocoding; [OSRM](https://project-osrm.org/) or [GraphHopper](https://www.graphhopper.com/open-source/) for routing; [Open-Meteo](https://open-meteo.com/) for weather; [REST Countries](https://restcountries.com/) for country metadata; [Frankfurter](https://frankfurter.dev/) for exchange rates; and [Wikivoyage/MediaWiki APIs](https://www.mediawiki.org/wiki/API:Main_page) for destination reference content.

Public demonstration endpoints and community services often impose usage limits and are unsuitable for unrestricted production traffic. Review terms, attribution and hosting options before integration. No flight, hotel or property live-price API is assumed. Flight, accommodation and property quotes start with manual entry or CSV/JSON import. Never scrape booking sites without permission.

The future `PriceProvider` contract returns source type (manual/import/API), provider, source URL, currency, amount, capture time, travel dates where applicable, confidence and expiration. Snapshots retain prior quotes for comparison. Refresh is configurable to 15 days, 30 days or manual only; missing providers preserve the last observation with an age label.
