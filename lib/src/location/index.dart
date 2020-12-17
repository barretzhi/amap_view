part of amap_core;

class Location {
  final double? latitude;

  final double? longitude;

  final String? address;

  final String? country;

  final String? city;

  final String? street;

  final String? district;

  final double? accuracy;

  /// 省
  final String? province;

  Location({
    this.latitude,
    this.longitude,
    this.accuracy,
    this.address,
    this.city,
    this.country,
    this.district,
    this.street,
    this.province,
  });

  factory Location.fromJson(Map<dynamic, dynamic> json) => _$LocationFromJson(json);

  Map<String, dynamic> toJson() => _$LocationToJson(this);
}

Location _$LocationFromJson(Map<dynamic, dynamic> json) {
  return Location(
    latitude: (json['latitude'] as num?)?.toDouble(),
    longitude: (json['longitude'] as num?)?.toDouble(),
    accuracy: (json['accuracy'] as num?)?.toDouble(),
    address: json['address'] as String?,
    city: json['city'] as String?,
    country: json['country'] as String?,
    district: json['district'] as String?,
    street: json['street'] as String?,
    province: json['province'] as String?,
  );
}

Map<String, dynamic> _$LocationToJson(Location instance) => <String, dynamic>{
      'latitude': instance.latitude,
      'longitude': instance.longitude,
      'address': instance.address,
      'country': instance.country,
      'city': instance.city,
      'street': instance.street,
      'district': instance.district,
      'accuracy': instance.accuracy,
      'province': instance.province,
    };
